// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    // 有 'apply false' 表示声明项目引入了插件，子项目需要根据实际情况决定是否应用此插件
    alias(libs.plugins.kotlin.compose) apply false
    // 没有 'apply false'，表示在根项目启用 Detekt, 既所有的子项目也会应用此插件
//    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}