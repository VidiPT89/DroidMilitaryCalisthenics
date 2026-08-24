# 🪖 DroidMilitaryCalisthenics

> Native Android training planner that builds personalized military calisthenics programs from your weight, height, age and goal.

[Report Bug](https://github.com/VidiPT89/DroidMilitaryCalisthenics/issues) · [Request Feature](https://github.com/VidiPT89/DroidMilitaryCalisthenics/issues)

## ✨ Features

- ✅ Guided onboarding — weight, height, age, sex, fitness level, goal, training days and equipment
- ✅ Periodized 6-week training plan generated on-device, no server required
- ✅ Warm-up, strength, circuit/HIIT, core and cool-down blocks every session
- ✅ Volume and intensity auto-calibrated from level, age and BMI signal
- ✅ Animated progress ring, day cards and completion states
- ✅ Tap any exercise for a looping stick-figure demo and a short coaching cue, correctly oriented (floor exercises horizontal, standing exercises upright)
- ✅ Log your bodyweight over time and watch the plan recalibrate itself automatically to your goal
- ✅ In-app PT-PT / EN language switch, independent of system locale
- ✅ Fully offline, plan and progress saved locally
- ✅ Dark, brand-consistent UI (orange / burnt yellow / near-black)
- ✅ Custom adaptive app icon matching the in-app splash mark
- ✅ Share any day, or export a full week as a PDF, straight from the plan screen
- ✅ Optional daily workout reminder notifications, with your choice of time

## 🛠️ Tech Stack

| Category | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM (`ViewModel` + `StateFlow`) |
| Persistence | Jetpack DataStore + kotlinx.serialization |
| Build | Gradle Kotlin DSL |

## 🚀 Quick Start

**Prerequisites**
- Android Studio Ladybug or newer
- JDK 17
- Android SDK Platform 35

**Setup**

```bash
git clone https://github.com/VidiPT89/DroidMilitaryCalisthenics.git
cd DroidMilitaryCalisthenics
./gradlew :app:assembleDebug
```

Open the project in Android Studio and run the `app` configuration on an
emulator or device (minimum SDK 26).

## 📖 Usage

1. Launch the app and fill in your profile: weight, height, age, level,
   goal, training days per week and available equipment.
2. Tap **Generate plan** to build your personalized 6-week program.
3. Browse weeks with the week selector, open a day to see its warm-up,
   strength, circuit, core and cool-down blocks, and mark workouts done
   as you complete them.
4. Switch language or edit your profile any time from Settings.

## 🧪 Testing

`PlanEngine` has a JVM unit test suite (`app/src/test`) covering plan
generation across the full input matrix (age, weight, level, goal,
equipment, days/week), level- and BMI-driven calibration, and the
weight-log recalibration path. A separate coverage test asserts every
day title and exercise name a generated plan can contain has both a
PT-PT and EN translation entry.

```bash
./gradlew :app:testDebugUnitTest
```

## 📄 License

Distributed under the MIT License. See [LICENSE](LICENSE) for details.

## 🔒 Privacy

All data stays on your device — nothing is ever transmitted anywhere. See [PRIVACY.md](PRIVACY.md) for details.

## 👨‍💻 Author

**David Arsénio Martins**
🌐 Website: [ividi.dev](https://ividi.dev)
🐙 GitHub: [@VidiPT89](https://github.com/VidiPT89)

## 🤝 Contributing

Issues and pull requests are welcome. For major changes, please open an
issue first to discuss what you'd like to change.

---

<p align="center">Developed by <a href="https://ividi.dev">David Arsénio Martins</a></p>
<p align="center">If you found this useful, consider giving it a ⭐</p>
