package dev.ividi.militarycalisthenics.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

enum class Lang { PT, EN }

private val strings: Map<String, Pair<String, String>> = mapOf(
    "app_title" to ("Calistenia Militar" to "Military Calisthenics"),
    "onboarding_title" to ("Cria o teu perfil" to "Build your profile"),
    "onboarding_subtitle" to ("Diz-nos quem és. Nós tratamos do plano." to "Tell us about yourself. We'll build the plan."),
    "weight" to ("Peso (kg)" to "Weight (kg)"),
    "height" to ("Altura (cm)" to "Height (cm)"),
    "age" to ("Idade" to "Age"),
    "sex" to ("Sexo" to "Sex"),
    "sex_male" to ("Masculino" to "Male"),
    "sex_female" to ("Feminino" to "Female"),
    "sex_unspecified" to ("Prefiro não dizer" to "Prefer not to say"),
    "level" to ("Nível" to "Level"),
    "level_beginner" to ("Iniciante" to "Beginner"),
    "level_intermediate" to ("Intermédio" to "Intermediate"),
    "level_advanced" to ("Avançado" to "Advanced"),
    "goal" to ("Objetivo" to "Goal"),
    "goal_fat_loss" to ("Perda de gordura" to "Fat loss"),
    "goal_strength_mass" to ("Força e massa" to "Strength & mass"),
    "goal_military_endurance" to ("Resistência militar" to "Military endurance"),
    "goal_mobility" to ("Mobilidade geral" to "General mobility"),
    "days_per_week" to ("Dias por semana" to "Days per week"),
    "equipment" to ("Equipamento" to "Equipment"),
    "equipment_bodyweight" to ("Só peso do corpo" to "Bodyweight only"),
    "equipment_bar" to ("Barra de tração" to "Pull-up bar"),
    "equipment_parallettes" to ("Paralelas" to "Parallettes"),
    "generate_plan" to ("Gerar plano" to "Generate plan"),
    "your_plan" to ("O teu plano" to "Your plan"),
    "week" to ("Semana" to "Week"),
    "day" to ("Dia" to "Day"),
    "warm_up" to ("Aquecimento" to "Warm-up"),
    "strength" to ("Força" to "Strength"),
    "circuit" to ("Circuito" to "Circuit"),
    "core" to ("Core" to "Core"),
    "cool_down" to ("Arrefecimento" to "Cool-down"),
    "sets" to ("séries" to "sets"),
    "reps" to ("reps" to "reps"),
    "seconds" to ("seg" to "sec"),
    "mark_done" to ("Marcar como feito" to "Mark as done"),
    "completed" to ("Concluído" to "Completed"),
    "edit_profile" to ("Editar perfil" to "Edit profile"),
    "about" to ("Sobre" to "About"),
    "developed_by" to ("Desenvolvido por David Arsénio Martins" to "Developed by David Arsénio Martins"),
    "website" to ("Website" to "Website"),
    "github" to ("GitHub" to "GitHub"),
    "start" to ("Começar" to "Get started"),
    "settings" to ("Definições" to "Settings"),
    "language" to ("Idioma" to "Language"),
    "error_range" to ("Valor fora do intervalo esperado" to "Value out of expected range")
)

fun t(key: String, lang: Lang): String {
    val pair = strings[key] ?: return key
    return if (lang == Lang.PT) pair.first else pair.second
}

val LocalLang = compositionLocalOf { Lang.PT }

@Composable
fun ProvideLang(lang: Lang, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLang provides lang, content = content)
}
