# Bollywood Studio: Android APK banane ka tareeka

Ye ek Android app hai jo poora Bollywood Video Studio phone par chalata hai. Koi server nahi chahiye.
Pehli baar Gemini API key paste karni hoti hai, phir app use yaad rakhta hai. Banaya hua video
`Downloads/BollywoodStudio/` mein save hota hai.

APK yahan (Claude ke andar) nahi ban sakta, kyunki yahan Android SDK aur internet nahi hai.
Isliye GitHub apne cloud mein APK banata hai. Aapko sirf files upload karni hain.

## Files

| File | Kaam |
| --- | --- |
| `index.html` | Poora studio (Brief, Agent, Assets, Video). |
| `MainActivity.java` | Android shell: file picker, video ko Downloads mein save karna. |
| `AndroidManifest.xml` | App ka naam aur permissions. |
| `build-apk.yml` | GitHub ka build script. Ye `.github/workflows/` folder mein jaata hai. |

## Phone se (sirf browser)

1. github.com par login karo (free account chalega). **New repository** banao, naam `bollywood-studio`.
2. Chrome ke menu mein **Desktop site** on karo. Repository mein **Add file → Upload files** dabao aur ye 3 files chuno:
   `index.html`, `MainActivity.java`, `AndroidManifest.xml`. Neeche **Commit changes**.
3. **Add file → Create new file** dabao. Naam ki jagah likho `.github/workflows/build-apk.yml`
   (slash likhte hi folder ban jaate hain). `build-apk.yml` ka poora text copy karke paste karo. **Commit changes**.
4. **Actions** tab kholo. "Build APK" chalta dikhega, 3 se 8 minute lagte hain. Hara tick aane par:
   - repository ke **Releases** mein `Bollywood-Studio.apk` download karo, ya
   - run ke page par neeche **Artifacts** mein `Bollywood-Studio-APK` (zip, andar `app-debug.apk`).
5. APK kholo. Phone **Install unknown apps** allow karne ko kahega. Allow karke Install dabao.
   Play Protect warning aaye toh **Install anyway** (ye aapka apna banaya debug app hai).
6. App kholo, Brief tab mein Gemini API key paste karo (aistudio.google.com/apikey), phir **Auto banao**.

## Computer se

Zip extract karo aur uska poora folder (`.github` folder samet) GitHub repository mein upload karo. Build apne aap shuru ho jaata hai.

## Dhyan rakhne wali baatein

- **Build abhi tak test nahi hua.** Java ka syntax check hua hai, workflow aur project banane wala step local chale hain,
  aur wahi studio page asli Chromium mein poori chain ke saath chala hai. Lekin Gradle build pehli baar GitHub par chalega.
  Fail ho jaye toh Actions mein us run ka aakhri error mujhe bhej do, main theek kar dunga.
- Android 10 ya naya chahiye.
- Ye **debug APK** hai. Har build ka naya debug key hota hai, isliye naya APK install karne se pehle purana app uninstall karo.
- App Google ko seedha calls karta hai, isliye key phone mein rehti hai. Uninstall karne par key hat jaati hai. Key kisi ko mat do.
- Image aur music models ke liye aksar billing wali key chahiye.
- Video real time mein record hota hai. Record ke dauran app screen on rakhta hai, app band mat karo.
- Bina APK ke bhi chal sakta hai: AI Studio se app share ya deploy karo, Chrome mein kholo aur **Add to Home screen** karo.
