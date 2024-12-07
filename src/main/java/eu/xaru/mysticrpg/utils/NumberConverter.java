package eu.xaru.mysticrpg.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * NumberConverter is a utility class that converts numerical digits into specific characters
 * based on a predefined mapping. This is used for displaying numbers with custom textures
 * via a texture pack.
 */
public class NumberConverter {

    // Mapping of digits to their corresponding characters
    private static final Map<Character, String> DIGIT_MAP = new HashMap<>();

    static {
        DIGIT_MAP.put('0', "ぁ");
        DIGIT_MAP.put('1', "あ");
        DIGIT_MAP.put('2', "ぃ");
        DIGIT_MAP.put('3', "い");
        DIGIT_MAP.put('4', "ぅ");
        DIGIT_MAP.put('5', "う");
        DIGIT_MAP.put('6', "ぇ");
        DIGIT_MAP.put('7', "え");
        DIGIT_MAP.put('8', "ぉ");
        DIGIT_MAP.put('9', "お");
    }

    /**
     * Converts an integer into a string with each digit replaced by its corresponding character.
     *
     * @param number The number to convert.
     * @return The converted string with custom characters.
     */
    public static String convert(int number) {
        String numberStr = String.valueOf(number);
        StringBuilder converted = new StringBuilder();

        for (char digit : numberStr.toCharArray()) {
            String replacement = DIGIT_MAP.get(digit);
            if (replacement != null) {
                converted.append(replacement);
            } else {
                // If the character is not a digit, append it as is.
                converted.append(digit);
            }
        }

        return converted.toString();
    }
}
