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
    "error_range" to ("Valor fora do intervalo esperado" to "Value out of expected range"),
    "close" to ("Fechar" to "Close"),
    "cue_push_up" to ("Mantém o corpo alinhado, desce até quase tocar no chão." to "Keep your body straight, lower until you nearly touch the floor."),
    "cue_squat" to ("Pés à largura dos ombros, desce como se fosses sentar numa cadeira." to "Feet shoulder-width apart, lower like sitting into a chair."),
    "cue_pull_up" to ("Pega firme na barra, puxa até o queixo passar a barra." to "Grip the bar firmly, pull until your chin clears the bar."),
    "cue_row" to ("Corpo reto, puxa o peito em direção à barra." to "Keep your body straight, pull your chest toward the bar."),
    "cue_dip" to ("Cotovelos para trás, desce até cerca de 90 graus." to "Elbows back, lower until your arms reach about 90 degrees."),
    "cue_lunge" to ("Dá um passo em frente, desce o joelho de trás quase ao chão." to "Step forward, lower your back knee almost to the floor."),
    "cue_burpee" to ("Agacha, estica para prancha, salta com as mãos acima da cabeça." to "Squat down, kick back to a plank, jump with hands overhead."),
    "cue_mountain_climber" to ("Em prancha, traz os joelhos ao peito alternadamente e depressa." to "From a plank, drive your knees to your chest quickly, alternating sides."),
    "cue_sprint" to ("De pé, corre no lugar levantando bem os joelhos." to "Standing tall, drive your knees up quickly as if sprinting on the spot."),
    "progress" to ("Progresso" to "Progress"),
    "log_weight" to ("Registar peso" to "Log weight"),
    "weight_history" to ("Histórico de peso" to "Weight history"),
    "log_weight_subtitle" to ("O teu plano ajusta-se automaticamente ao novo peso." to "Your plan recalibrates automatically to the new weight."),
    "save" to ("Guardar" to "Save"),
    "cancel" to ("Cancelar" to "Cancel"),
    "no_weight_history" to ("Ainda sem registos de peso." to "No weight entries yet."),
    "plan_recalibrated" to ("Plano recalibrado com o novo peso." to "Plan recalibrated with your new weight."),
    "cue_plank" to ("Corpo reto da cabeça aos calcanhares, core contraído." to "Keep a straight line from head to heels, core braced."),
    "cue_leg_raise" to ("Deitado, sobe as pernas esticadas sem levantar as costas." to "Lying down, raise your straight legs without lifting your lower back."),
    "cue_twist" to ("Sentado, gira o tronco de um lado para o outro." to "Seated, rotate your torso from side to side."),
    "cue_jumping_jack" to ("Salta abrindo pernas e braços ao mesmo tempo." to "Jump while spreading your arms and legs at the same time."),
    "cue_stretch" to ("Movimento suave e controlado, sem forçar a articulação." to "Move slowly and with control, never forcing the joint."),
    "share_plan" to ("Partilhar" to "Share"),
    "reminders" to ("Lembretes de treino" to "Workout reminders"),
    "reminders_subtitle" to ("Recebe uma notificação diária à hora escolhida." to "Get a daily notification at the chosen time."),
    "reminder_time" to ("Hora do lembrete" to "Reminder time"),
    "reminder_notification_title" to ("Hora de treinar" to "Time to train"),
    "reminder_notification_body" to ("O teu plano de calistenia militar está à tua espera." to "Your military calisthenics plan is waiting for you."),
    "notifications_denied" to ("Permite notificações nas definições do sistema para receber lembretes." to "Allow notifications in system settings to receive reminders."),
    "pending" to ("Pendente" to "Pending")
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
