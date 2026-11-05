"""Generate placeholder-but-valid UI audio assets for Zazili.

Produces:
  - app/src/main/res/raw/ui_pop.wav      (short pop SFX for button taps)
  - app/src/main/res/raw/bgm_theme.wav   (seamless 4s looping background pad)

Both are 16-bit mono PCM WAV files so they load via SoundPool / MediaPlayer
with zero external dependencies. Run with the bundled Python (numpy included).
"""
import math
import os
import wave

import numpy as np

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RAW_DIR = os.path.join(ROOT, "app", "src", "main", "res", "raw")
os.makedirs(RAW_DIR, exist_ok=True)


def write_wav(path: str, samples: np.ndarray, rate: int) -> None:
    samples = np.clip(samples, -1.0, 1.0)
    pcm = (samples * 32767.0).astype("<i2")
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(rate)
        w.writeframes(pcm.tobytes())


def gen_pop() -> None:
    rate = 44100
    dur = 0.16
    t = np.linspace(0.0, dur, int(rate * dur), endpoint=False)
    # Downward chirp (900 -> 320 Hz) with a fast exponential decay = "pop".
    freq = np.linspace(900.0, 320.0, t.size)
    phase = 2.0 * math.pi * np.cumsum(freq) / rate
    env = np.exp(-t * 28.0)
    pop = np.sin(phase) * env
    # Tiny transient click for the "pop" attack.
    click = int(0.002 * rate)
    pop[:click] += np.linspace(0.35, 0.0, click)
    write_wav(os.path.join(RAW_DIR, "ui_pop.wav"), pop, rate)


def gen_bgm() -> None:
    rate = 22050
    dur = 4.0
    n = int(rate * dur)
    t = np.linspace(0.0, dur, n, endpoint=False)
    # sin^2 envelope -> amplitude (and derivative) zero at loop boundaries.
    env = np.sin(math.pi * t / dur) ** 2

    # Frequencies are integer multiples of 1/dur so the loop is click-free.
    root = 220.5  # A3, exactly 882 cycles in 4s
    fifth = 330.75  # E4, exactly 1323 cycles in 4s

    voice_root = (
        np.sin(2 * math.pi * root * t)
        + 0.5 * np.sin(2 * math.pi * root * 2 * t)
        + 0.35 * np.sin(2 * math.pi * root * 3 * t)
    )
    voice_fifth = (
        np.sin(2 * math.pi * fifth * t)
        + 0.4 * np.sin(2 * math.pi * fifth * 2 * t)
    )
    bgm = (0.5 * voice_root + 0.3 * voice_fifth) * env * 0.32
    write_wav(os.path.join(RAW_DIR, "bgm_theme.wav"), bgm, rate)


if __name__ == "__main__":
    gen_pop()
    gen_bgm()
    print("Audio written to", RAW_DIR)
