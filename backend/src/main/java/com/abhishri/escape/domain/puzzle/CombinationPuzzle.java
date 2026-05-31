package com.abhishri.escape.domain.puzzle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.Map;

@Entity
@Table(name = "COMBINATION_PUZZLE")
public class CombinationPuzzle extends Puzzle {

    @Column(name = "EXPECTED_CODE", nullable = false, length = 32)
    private String expectedCode;

    @Column(name = "DIGIT_COUNT", nullable = false)
    private int digitCount;

    @Override
    public boolean attempt(Map<String, String> inputs) {
        String submitted = inputs.get("code");
        if (submitted == null) return false;
        return expectedCode.equals(submitted);
    }

    public String getExpectedCode() { return expectedCode; }
    public void setExpectedCode(String expectedCode) { this.expectedCode = expectedCode; }

    public int getDigitCount() { return digitCount; }
    public void setDigitCount(int digitCount) { this.digitCount = digitCount; }
}
