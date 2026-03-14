package com.G7.CTBS.util;

public class TextUtils {
    /**
     * Hàm chuẩn hóa chuỗi: Viết hoa chữ cái đầu của mỗi từ.
     * Ví dụ: "  hÀnh   ĐỘNG  " -> "Hành Động"
     */
    public static String formatTitleCase(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }
        
        // xóa khoảng trắng thừa ở 2 đầu và giữa các từ
        String normalizedInput = input.trim().replaceAll("\\s+", " ");
        
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        
        // duyệt qua từng ký tự để chuẩn hóa
        for (char c : normalizedInput.toCharArray()) {
            // nếu gặp khoảng trắng hoặc dấu gạch ngang, báo hiệu ký tự tiếp theo cần viết hoa
            if (Character.isSpaceChar(c) || c == '-') {
                result.append(c);
                capitalizeNext = true;
            } else if (capitalizeNext) {
                // viết hoa chữ cái đầu từ
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                // viết thường các chữ cái còn lại
                result.append(Character.toLowerCase(c));
            }
        }
        
        return result.toString();
    }
}
