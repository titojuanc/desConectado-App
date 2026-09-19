// Los plugins se declaran aquí sin aplicarse; cada módulo aplica los que necesita.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.services) apply false
}
