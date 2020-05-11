package com.aritrosaha.aritr.communityprojectapptest;

import androidx.annotation.Keep;

@Keep
class CustomTools {
    // Method to convert the string
    @Keep
    static String capitailizeWord(String str) {
        if (str == null){
            return str;
        }

        StringBuffer s = new StringBuffer();

        // Declare a character of space
        // To identify that the next character is the starting
        // of a new word
        char ch = ' ';
        for (int i = 0; i < str.length(); i++) {

            // If previous character is space and current
            // character is not space then it shows that
            // current letter is the starting of the word
            if (ch == ' ' && str.charAt(i) != ' ')
                s.append(Character.toUpperCase(str.charAt(i)));
            else
                s.append(str.charAt(i));
            ch = str.charAt(i);
        }

        // Return the string with trimming
        //Log.d("CustomTools", s.toString().trim());
        return s.toString().trim();
    }
}
