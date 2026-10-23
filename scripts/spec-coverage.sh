#!/usr/bin/env bash
# Покрытие спеки устройства проверками (IS532, Э8): адреса строк AWP-NN.k
# из docs/handbook/specs/words-add-from-photo/spec.md против имён тестов
# Kotlin (`AWP-NN_k` — JVM не пускает точку в имя метода; в
# инструментальных тестах бывает строчными без дефиса — `awp20_1_`) в
# modules, app, core и документов ручников. Копия
# backend/scripts/spec-coverage.sh с параметрами «файл спеки» и «префикс».
#
# Строки «снято» — не проверки, в покрытии не участвуют.
#
# Выход: по тезису — закрытые и незакрытые адреса; код 1, если у
# тезиса со статусом «реализовано» есть незакрытая строка, или
# если в тестах есть адрес, которого нет в спеке.
#
#   SPEC_FILE=… SPEC_PREFIX=… — другая спека и префикс (по умолчанию AWP);
#   SPEC_MANUALS="file1.md file2.md" — документы ручников с адресами;
#   SPEC_SIDE=устройство — своя сторона для продуктовой спеки бэкенда
#     (через backend/): судятся тезисы «Исполняет: устройство» и «оба»,
#     тезисы другой стороны пропускаются; без поля «Исполняет» (экранные
#     спеки) тезис считается своим;
#   SPEC_WHERE=1 — вместо сводки печатать карту «адрес → файл» по
#     одной строке на пару (ревью К18: какой тест закрывает строку).
#
# Инструменты: grep и perl, без python/sed/awk (правило репозитория).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SPEC="${SPEC_FILE:-$ROOT/docs/handbook/specs/words-add-from-photo/spec.md}"
PREFIX="${SPEC_PREFIX:-AWP}"
SIDE="${SPEC_SIDE:-устройство}"

perl -CSDA -Mutf8 -e '
  use strict; use warnings;
  my ($spec, $root, $prefix, $manuals, $side, $where) = @ARGV;

  # Адреса спеки: тезис → статус, сторона, список номеров строк.
  open my $fh, "<:encoding(UTF-8)", $spec or die "spec: $!";
  my (%status, %who, %lines, %all, @order, $cur);
  while (my $l = <$fh>) {
    if ($l =~ /^#### ($prefix-\d+) /) { $cur = $1; push @order, $cur; $lines{$cur} = []; next }
    next unless defined $cur;
    if ($l =~ /Исполняет: (\S+)/ && !exists $who{$cur}) { $who{$cur} = $1 }
    if ($l =~ /Статус: (\S+)/ && !exists $status{$cur}) { $status{$cur} = $1; next }
    if ($l =~ /^- \[(\d+)\]/) { $all{"$cur.$1"} = 1; push @{$lines{$cur}}, $1 unless $l =~ /^- \[\d+\] снято\b/ }
  }
  close $fh;

  # Своя сторона: без поля — своё; «оба» судится каждой стороной целиком.
  my @mine = grep { !exists $who{$_} || $who{$_} eq $side || $who{$_} eq "оба" } @order;
  my $skipped = @order - @mine;

  # Адреса в тестах Kotlin трёх каталогов и в ручниках: оба разделителя.
  my %covered;
  my @files;
  for my $dir ("modules", "app", "core") {
    push @files, grep { /Test\.kt$/ } split /\n/, qx(grep -rl --include=*Test.kt "$prefix-" "$root/$dir" 2>/dev/null);
  }
  push @files, grep { length } split /\s+/, $manuals;
  for my $f (@files) {
    open my $t, "<:encoding(UTF-8)", $f or next;
    while (my $l = <$t>) {
      while ($l =~ /\b($prefix)-?(\d+)[._](\d+)/gi) { $covered{uc($1) . "-$2.$3"}{$f}++ }
    }
    close $t;
  }

  # SPEC_WHERE=1: карта «адрес → файл», по строке на пару, адреса по номерам.
  if ($where) {
    my $num = sub { my @n = $_[0] =~ /(\d+)\.(\d+)/; return @n };
    for my $addr (sort { my @a = $num->($a); my @b = $num->($b); $a[0] <=> $b[0] || $a[1] <=> $b[1] } keys %covered) {
      print "$addr\t$_\n" for sort keys %{$covered{$addr}};
    }
    exit 0;
  }

  my ($bad, $closed_total, $open_total) = (0, 0, 0);
  for my $id (@mine) {
    my $st = $status{$id} // "?";
    my (@closed, @open);
    for my $n (@{$lines{$id}}) { exists $covered{"$id.$n"} ? push(@closed, $n) : push(@open, $n) }
    $closed_total += @closed; $open_total += @open;
    my $mark = (@open && $st eq "реализовано") ? "!!" : "  ";
    $bad = 1 if $mark eq "!!";
    printf "%s %-7s %-12s %-11s закрыто %-16s открыто %s\n", $mark, $id, $st, ($who{$id} // "-"),
      (join(",", @closed) || "-"), (join(",", @open) || "-");
  }

  # Адреса в тестах, которых нет в спеке (по всем тезисам, не только своим; «снято» — есть).
  my @stale = sort grep { !$all{$_} } keys %covered;
  if (@stale) { $bad = 1; print "\nВ тестах есть адреса, которых нет в спеке: @stale\n" }

  printf "\nСторона «%s»: тезисов %d, другой стороны пропущено %d. Строк закрыто %d, открыто %d. %s\n",
    $side, scalar(@mine), $skipped, $closed_total, $open_total,
    $bad ? "ЕСТЬ НАРУШЕНИЯ (!! — реализовано с открытыми строками)" : "нарушений нет";
  exit($bad ? 1 : 0);
' "$SPEC" "$ROOT" "$PREFIX" "${SPEC_MANUALS:-}" "$SIDE" "${SPEC_WHERE:-}"
