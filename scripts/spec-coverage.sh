#!/usr/bin/env bash
# Покрытие спеки устройства проверками (IS532, Э8): адреса строк AWP-NN.k
# из docs/handbook/specs/words-add-from-photo/spec.md против имён тестов
# Kotlin (`AWP-NN_k` — JVM не пускает точку в имя метода) в modules, app,
# core и документов ручников. Копия backend/scripts/spec-coverage.sh с
# параметрами «файл спеки» и «префикс».
#
# Выход: по утверждению — закрытые и незакрытые адреса; код 1, если у
# утверждения со статусом «реализовано» есть незакрытая строка, или
# если в тестах есть адрес, которого нет в спеке.
#
#   SPEC_FILE=… SPEC_PREFIX=… — другая спека и префикс (по умолчанию AWP);
#   SPEC_MANUALS="file1.md file2.md" — документы ручников с адресами.
#
# Инструменты: grep и perl, без python/sed/awk (правило репозитория).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SPEC="${SPEC_FILE:-$ROOT/docs/handbook/specs/words-add-from-photo/spec.md}"
PREFIX="${SPEC_PREFIX:-AWP}"

perl -CSD -Mutf8 -e '
  use strict; use warnings;
  my ($spec, $root, $prefix, $manuals) = @ARGV;

  # Адреса спеки: утверждение → статус, список номеров строк.
  open my $fh, "<:encoding(UTF-8)", $spec or die "spec: $!";
  my (%status, %lines, @order, $cur);
  while (my $l = <$fh>) {
    if ($l =~ /^#### ($prefix-\d+) /) { $cur = $1; push @order, $cur; $lines{$cur} = []; next }
    next unless defined $cur;
    if ($l =~ /Статус: (\S+)/ && !exists $status{$cur}) { $status{$cur} = $1; next }
    if ($l =~ /^- \[(\d+)\]/) { push @{$lines{$cur}}, $1 }
  }
  close $fh;

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
      while ($l =~ /($prefix-\d+)[._](\d+)/g) { $covered{"$1.$2"}{$f}++ }
    }
    close $t;
  }

  my ($bad, $closed_total, $open_total) = (0, 0, 0);
  for my $id (@order) {
    my $st = $status{$id} // "?";
    my (@closed, @open);
    for my $n (@{$lines{$id}}) { exists $covered{"$id.$n"} ? push(@closed, $n) : push(@open, $n) }
    $closed_total += @closed; $open_total += @open;
    my $mark = (@open && $st eq "реализовано") ? "!!" : "  ";
    $bad = 1 if $mark eq "!!";
    printf "%s %-7s %-12s закрыто %-16s открыто %s\n", $mark, $id, $st,
      (join(",", @closed) || "-"), (join(",", @open) || "-");
  }

  # Адреса в тестах, которых нет в спеке.
  my %known; for my $id (keys %lines) { $known{"$id.$_"} = 1 for @{$lines{$id}} }
  my @stale = sort grep { !$known{$_} } keys %covered;
  if (@stale) { $bad = 1; print "\nВ тестах есть адреса, которых нет в спеке: @stale\n" }

  printf "\nСтрок закрыто %d, открыто %d. %s\n", $closed_total, $open_total,
    $bad ? "ЕСТЬ НАРУШЕНИЯ (!! — реализовано с открытыми строками)" : "нарушений нет";
  exit($bad ? 1 : 0);
' "$SPEC" "$ROOT" "$PREFIX" "${SPEC_MANUALS:-}"
