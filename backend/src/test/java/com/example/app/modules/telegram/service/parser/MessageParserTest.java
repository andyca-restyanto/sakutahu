// filepath: /backend/src/test/java/com/example/app/modules/telegram/service/parser/MessageParserTest.java
package com.example.app.modules.telegram.service.parser;

import com.example.app.modules.category.entity.TransactionType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class MessageParserTest {

    @ParameterizedTest(name = "{0} → {1} | {2}")
    @CsvSource(delimiter = '|', value = {
            "kopi susu 25rb        | 25000      | kopi susu",
            "kopi 25k              | 25000      | kopi",
            "bensin 30000          | 30000      | bensin",
            "Rp25.000 parkir       | 25000      | parkir",
            "parkir Rp 5.000       | 5000       | parkir",
            "1,5jt servis          | 1500000    | servis",
            "1.5jt servis          | 1500000    | servis",
            "gaji oktober 5 juta   | 5000000    | gaji oktober",
            "25.000,50 jus         | 25000.50   | jus",
            "beli 2 kopi 25rb      | 25000      | beli 2 kopi",
            "bensin 30000 2 liter  | 30000      | bensin 2 liter",
            "25,000 makan          | 25000      | makan",
            "cicilan 1.250.000     | 1250000    | cicilan",
            "2x kopi 15rb          | 15000      | 2x kopi",
            "25.5k snack           | 25500      | snack",
            "makan 20 ribu         | 20000      | makan",
    })
    void parsesAmountAndNote(String text, String amount, String note) {
        var result = MessageParser.parse(text);
        assertThat(result).isInstanceOf(MessageParser.Entry.class);
        var entry = (MessageParser.Entry) result;
        assertThat(entry.amount()).isEqualByComparingTo(amount);
        assertThat(entry.note()).isEqualTo(note);
        assertThat(entry.explicitType()).isNull();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "+gaji 5jt          | INCOME  | 5000000 | gaji",
            "/masuk 5jt gaji    | INCOME  | 5000000 | gaji",
            "/keluar 25000 kopi | EXPENSE | 25000   | kopi",
            "- 15rb parkir      | EXPENSE | 15000   | parkir",
    })
    void explicitType(String text, TransactionType type, String amount, String note) {
        var entry = (MessageParser.Entry) MessageParser.parse(text);
        assertThat(entry.explicitType()).isEqualTo(type);
        assertThat(entry.amount()).isEqualByComparingTo(amount);
        assertThat(entry.note()).isEqualTo(note);
    }

    @ParameterizedTest
    @ValueSource(strings = {"kemarin kopi 20rb", "kopi 20rb 12/10", "tgl 5 bayar 20rb", "besok bensin 30rb",
            "kmrn parkir 5rb", "listrik bulan lalu 300rb"})
    void otherDatesAreRejected(String text) {
        assertThat(MessageParser.parse(text)).isInstanceOf(MessageParser.DateNotAllowed.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"kopi susu", "0 rb", "", "   ", "rp"})
    void missingAmount(String text) {
        assertThat(MessageParser.parse(text)).isInstanceOf(MessageParser.MissingAmount.class);
    }
}
