package com.city.utils;
import org.junit.jupiter.api.Test;
import java.util.Locale;
import static org.assertj.core.api.Assertions.*;
class SearchTextUtilsTest {
 @Test void handlesMissingAndWhitespaceKeywords() {
  assertThat(SearchTextUtils.normalizeKeyword(null)).isEmpty();
  assertThat(SearchTextUtils.normalizeKeyword("  ")).isEmpty();
  assertThat(SearchTextUtils.containsKeyword(null,"hello")).isFalse();
  assertThat(SearchTextUtils.containsKeyword("hello",null)).isFalse();
  assertThat(SearchTextUtils.containsKeyword("hello","")).isTrue();
 }
 @Test void matchesUnicodeAndIgnoresHostLocale() {
  Locale old=Locale.getDefault();
  try {
   Locale.setDefault(Locale.forLanguageTag("tr-TR"));
   String keyword=SearchTextUtils.normalizeKeyword("  CITY 音乐  ");
   assertThat(keyword).isEqualTo("city 音乐");
   assertThat(SearchTextUtils.containsKeyword("CITY 音乐节",keyword)).isTrue();
   assertThat(SearchTextUtils.containsKeyword("城市戏曲",keyword)).isFalse();
  } finally {Locale.setDefault(old);}
 }
}
