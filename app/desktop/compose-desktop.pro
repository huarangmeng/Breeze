# 防止 ProGuard 因第三方 latex/markdown 库内部缺失字段或方法而失败。
# 这些库的 release jar 自身存在引用与字段命名不一致的问题，整体 keep 即可。
-dontwarn com.hrm.latex.**
-dontwarn com.hrm.codehighlight.**
-dontwarn com.hrm.markdown.**
-keep class com.hrm.latex.** { *; }
-keep class com.hrm.codehighlight.** { *; }
-keep class com.hrm.markdown.** { *; }
