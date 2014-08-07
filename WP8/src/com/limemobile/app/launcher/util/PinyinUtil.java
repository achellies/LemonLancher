package com.limemobile.app.launcher.util;

import android.os.Build;

import com.limemobile.app.launcher.util.Hanzi2Pinyin1.Token;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class PinyinUtil {
    
    public static String toString(String hanzi) {
        String pinyin = "";
        
        ArrayList<Token> tokens = null;
        if (Build.VERSION.SDK_INT >= 14)
            tokens = Hanzi2Pinyin14.getInstance().get(hanzi);
        else
            tokens = Hanzi2Pinyin1.getInstance().get(hanzi);
        if (tokens != null && tokens.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (Token token : tokens) {
                // Put Chinese character's pinyin, then proceed with the
                // character itself.
                if (Token.PINYIN == token.type) {
                    if (sb.length() > 0) {
                        sb.append(' ');
                    }
                    sb.append(token.target);
                    sb.append(' ');
                    sb.append(token.source);
                } else {
                    if (sb.length() > 0) {
                        sb.append(' ');
                    }
                    sb.append(token.source);
                }
            }
            pinyin = sb.toString();
        }
        return pinyin.toLowerCase();
    }
    
    public static String getFirstLetter(String str) {
        String fallbackLetter = "#";
        if (str == null)
            return fallbackLetter;

        if (str.trim().length() == 0)
            return "#";

        char c = str.trim().substring(0, 1).charAt(0);
        // 正则表达式，判断首字母是否是英文字母
        Pattern pattern = Pattern.compile("^[A-Za-z]+$");
        if (pattern.matcher(c + "").matches())
            return (c + "").toLowerCase();
        
        return fallbackLetter;
    }
}
