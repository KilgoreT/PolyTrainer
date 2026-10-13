#!/usr/bin/env perl
# Пятёрка видимых лексем-кандидатов на лемму (AWP-05): по одной на каждую
# часть речи в порядке сервера, затем добор до пяти. Вход — записанный
# ответ прода (prod_v0.1.9/*.json).
use strict;
use warnings;
use JSON::PP;
binmode STDOUT, ':encoding(UTF-8)';

local $/;
open my $fh, '<:raw', $ARGV[0] or die "open: $!";
my $j = decode_json(<$fh>);
close $fh;

for my $w (@{ $j->{words} }) {
    for my $l (@{ $w->{lemmas} // [] }) {
        my @lx = @{ $l->{lexemes} // [] };
        my (%seen, %taken, @five);
        for my $i (0 .. $#lx) {
            my $p = $lx[$i]{partOfSpeechKey} // 'null';
            next if $seen{$p}++;
            push @five, $i;
            $taken{$i} = 1;
            last if @five == 5;
        }
        for my $i (0 .. $#lx) {
            last if @five == 5;
            next if $taken{$i};
            push @five, $i;
            $taken{$i} = 1;
        }
        my %byPos;
        $byPos{ $_->{partOfSpeechKey} // 'null' }++ for @lx;
        printf "%s -> %s (%d lexemes: %s; formNote=%s)\n",
            $w->{text}, $l->{text}, scalar @lx,
            join(', ', map { "$_=$byPos{$_}" } sort { $byPos{$b} <=> $byPos{$a} } keys %byPos),
            $l->{formNote} // '-';
        for my $i (@five) {
            my $x = $lx[$i];
            printf "  %-28s %-12s %-14s tr=[%s] gloss=%s cap=%s parent=%s notes=[%s] ex=%d\n",
                $x->{key}, $x->{partOfSpeechKey} // 'null', $x->{source},
                join(', ', @{ $x->{translations} // [] }),
                substr($x->{gloss} // '', 0, 60),
                $x->{caption} // '',
                $x->{parentKey} // '',
                join(', ', @{ $x->{notes} // [] }),
                scalar @{ $x->{examples} // [] };
        }
    }
}
