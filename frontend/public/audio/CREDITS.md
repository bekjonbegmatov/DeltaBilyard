# Recorded game sounds

All eight sound recordings are released by their authors under [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/). Their primary Freesound pages and license links were checked on 2026-11-08. These recordings may be copied, adapted, and redistributed, including commercially. Credits are retained here even though CC0 does not require attribution.

| Local file | Author and original recording | Source excerpt |
| --- | --- | --- |
| `ball-contact.wav` | Za-Games — [billiard ball clack](https://freesound.org/people/Za-Games/sounds/539854/) | 0–0.239 s |
| `cue-strike.wav` | AmberdeMeillon — [Pool_Table_Ball_Hit_Sink.WAV](https://freesound.org/people/AmberdeMeillon/sounds/443066/) | 3.7618–4.09 s; initial cue impact, before the subsequent ball collision |
| `rail-contact.wav` | mccarthy@bedmas.com — [pool-ball-bounce-off-rail.wav](https://freesound.org/people/mccarthy%40bedmas.com/sounds/42364/) | 0.3768–0.62 s |
| `pocket-drop.wav` | AmberdeMeillon — [Pool_Table_Ball_Hit.WAV](https://freesound.org/people/AmberdeMeillon/sounds/443067/) | 5.0666–5.45 s; pocket impact after the initial cue strike |
| `laughter.wav` | MuzakPlz — [Female Laughter](https://freesound.org/people/MuzakPlz/sounds/625135/) | 4.49–5.45 s |
| `hand-clap.wav` | steeltowngaming — [Clap 01](https://freesound.org/people/steeltowngaming/sounds/537735/) | 0.0929–0.25 s; played as a short sequence for applause |
| `wow.wav` | Lunardrive — [WOW_SoundSmith.wav](https://freesound.org/people/Lunardrive/sounds/48981/) | 0.08–0.92 s |
| `cheer.wav` | jayfrosting — [Cheer 1 short.wav](https://freesound.org/people/jayfrosting/sounds/333405/) | 0–1.95 s |

The source file titles in the two AmberdeMeillon entries are preserved exactly; the description of `Pool_Table_Ball_Hit.WAV` identifies a ball dropping into the pocket.

## Processing and playback

The authors' public high-quality previews were decoded, trimmed to the excerpts above, downmixed to mono, resampled to 32 kHz, high-pass filtered at 70 Hz to reduce rumble, peak-normalized to −3 dBFS, and given short edge fades. The distributed files are 16-bit PCM WAV, totaling 326,808 bytes. `sources.json` includes source preview URLs, source and output SHA-256 hashes, duration, and transformation metadata.

Game sounds use the actual recorded impacts. Small playback-rate variations reduce repetition. A separate game-sound limiter bounds simultaneous break-shot transients; it does not process LiveKit voice chat. 😂 uses recorded laughter; 👏 and 🤝 use the recorded hand clap; 😮 uses the recorded “wow”; 🔥, 🎯 and 😎 use recorded cheering. No speech synthesis, voice cloning, or microphone recording is used for these effects. All assets are served from this application's `/audio/` path; runtime playback does not contact Freesound.

Audio loading and decoding are silent. Playback starts only after a browser-accepted user gesture. Background suspension and slow asset loads do not enqueue old impacts. A failed asset fetch can be retried after five seconds on the next audio unlock/preload.

## Haptics

`haptics.ts` uses the documented [Telegram HapticFeedback API](https://core.telegram.org/bots/webapps#hapticfeedback) only when `isVersionAtLeast('6.1')` passes. Selection changes, ordinary taps, cue strikes, significant collisions, and successful pots use separate feedback types, with rate limits for rapid events. Outside a supported Telegram bridge, Android may use `navigator.vibrate`.

Haptics depend on hardware and system/Telegram settings. iOS browser vibration is not a fallback for disabled Telegram haptics. Physical feedback must be checked on real devices; API tests alone cannot establish that a motor vibrated.
