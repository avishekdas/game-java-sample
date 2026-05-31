package com.abhishri.escape.domain.puzzle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.Map;

@Entity
@Table(name = "RIDDLE_PUZZLE")
public class RiddlePuzzle extends Puzzle {

    @Column(name = "QUESTION_TEXT", columnDefinition = "CLOB", nullable = false)
    private String questionText;

    @Column(name = "EXPECTED_ANSWER", nullable = false, length = 255)
    private String expectedAnswer;

    @Column(name = "CASE_SENSITIVE", nullable = false)
    private boolean caseSensitive;

    @Override
    public boolean attempt(Map<String, String> inputs) {
        String submitted = inputs.get("answer");
        if (submitted == null) return false;
        submitted = submitted.trim();
        if (caseSensitive) {
            return expectedAnswer.equals(submitted);
        }
        return expectedAnswer.equalsIgnoreCase(submitted);
    }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public String getExpectedAnswer() { return expectedAnswer; }
    public void setExpectedAnswer(String expectedAnswer) { this.expectedAnswer = expectedAnswer; }

    public boolean isCaseSensitive() { return caseSensitive; }
    public void setCaseSensitive(boolean caseSensitive) { this.caseSensitive = caseSensitive; }
}
