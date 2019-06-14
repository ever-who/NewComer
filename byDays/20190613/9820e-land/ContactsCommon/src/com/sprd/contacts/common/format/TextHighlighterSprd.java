
package com.sprd.contacts.common.format;

import com.android.contacts.common.format.FormatUtils;

import android.text.SpannableString;
import android.text.style.CharacterStyle;

public abstract class TextHighlighterSprd {

    public TextHighlighterSprd(int textStyle) {

    }

    /**
     * Returns a CharSequence which highlights the given prefix if found in the
     * given text or textWithSpell.
     * 
     * @param text the display name of contact to which to apply the highlight
     * @param textWithSpell the text coming from column in table raw_contacts
     * @param prefix the prefix to look for
     */
    public CharSequence applyPrefixHighlight(CharSequence text, CharSequence textWithSpell, char[] prefix) {
        int index = -1;
        int[] length = {
                1
        };
        boolean textIsChinese = false;
        boolean prefixIsChinese = false;
        if (prefix != null) {
            textIsChinese = FormatUtils.isChinese(text.toString());
            prefixIsChinese = FormatUtils.isChinese(String.valueOf(prefix));
            if (textIsChinese && !prefixIsChinese) {
                index = FormatUtils.indexOfSpellSearch(text, textWithSpell, prefix, length);
            } else {
                index = FormatUtils.indexOfWordPrefix(text, String.valueOf(prefix), length);
            }
        }
        if (index != -1) {
            SpannableString result = new SpannableString(text);
            if (!prefixIsChinese
                    && textIsChinese) {
                int textLength = text.length();

                index = convertIndex(text, textLength, index, 0, true);
                length[0] = convertIndex(text, textLength, index, length[0], false);
                result.setSpan(getCurrentStyleSpan(), index, length[0], 0 /* flags */);
            } else {
                result.setSpan(getCurrentStyleSpan(), index, index + length[0], 0 /* flags */);
            }
            return result;
        } else {
            return text;
        }
    }

    /**
     * The return value of {@link FormatUtils.indexOfSpellSearch} and
     * {@linkFormatUtils.indexOfWordPrefix} is string number,so convert them
     * into Character number without blank and dot is needed.
     * 
     * @param text the display name of contact to which to apply the highlight
     * @param index the index of the first string that starts with the given
     *            prefix
     * @param length the string number matching with the prefix
     * @param isIndex true for convert index or false for convert length
     */
    private int convertIndex(CharSequence text, int textLength, int index, int length,
            boolean isIndex) {
        int indexValue = isIndex ? 0 : index;
        int indexOrLength = isIndex ? index : length;
        int sign = 0;
        int strNum = 0;
        for (; indexValue < textLength; indexValue++) {
            if (indexOrLength == 0 && isIndex) {
                break;
            } else {
                if (FormatUtils.isChinese(text.charAt(indexValue))) {
                    strNum++;
                    sign = 0;
                } else if (Character.isLetterOrDigit(text.charAt(indexValue))) {
                    if (sign == 0) {
                        strNum++;
                        sign++;
                    }
                    if (indexValue + 1 == textLength) {
                        break;
                    }
                    if (!FormatUtils.isChinese(text.charAt(indexValue + 1))
                            && Character.isLetterOrDigit(text.charAt(indexValue + 1))) {
                        continue;
                    }
                } else {
                    sign = 0;
                }
                if (strNum == indexOrLength) {
                    break;
                }

            }
        }

        if (isIndex) {
            index = index == 0 ? index : indexValue + 1;
            return index;
        } else {
            return indexValue + 1;
        }
    }

    protected abstract CharacterStyle getCurrentStyleSpan();
}
