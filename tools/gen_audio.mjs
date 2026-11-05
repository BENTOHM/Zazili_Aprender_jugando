import fs from "fs";

const SR = 22050;
const OUT = "app/src/main/res/raw";

function writeWav(path, samples, peak = 0.85, fadeInMs = 5, fadeOutMs = 40) {
  const n = samples.length;
  let max = 0;
  for (let i = 0; i < n; i++) max = Math.max(max, Math.abs(samples[i]));
  const g = max > 0 ? peak / max : 1;
  const fi = Math.floor((fadeInMs / 1000) * SR);
  const fo = Math.floor((fadeOutMs / 1000) * SR);
  const buf = Buffer.alloc(44 + n * 2);
  buf.write("RIFF", 0);
  buf.writeUInt32LE(36 + n * 2, 4);
  buf.write("WAVE", 8);
  buf.write("fmt ", 12);
  buf.writeUInt32LE(16, 16);
  buf.writeUInt16LE(1, 20);
  buf.writeUInt16LE(1, 22);
  buf.writeUInt32LE(SR, 24);
  buf.writeUInt32LE(SR * 2, 28);
  buf.writeUInt16LE(2, 32);
  buf.writeUInt16LE(16, 34);
  buf.write("data", 36);
  buf.writeUInt32LE(n * 2, 40);
  for (let i = 0; i < n; i++) {
    let env = 1;
    if (i < fi) env = i / fi;
    if (i > n - fo) env = Math.min(env, (n - i) / fo);
    let v = Math.max(-1, Math.min(1, samples[i] * g * env));
    buf.writeInt16LE(Math.round(v * 32767), 44 + i * 2);
  }
  fs.writeFileSync(path, buf);
  console.log(path, (n / SR).toFixed(2) + "s", (buf.length / 1024).toFixed(0) + "KB");
}

const F = {
  F2: 87.31, G2: 98.0, A2: 110.0,
  C3: 130.81, D3: 146.83, E3: 164.81, F3: 174.61, G3: 196.0,
  A3: 220.0,
  C4: 261.63, D4: 293.66, E4: 329.63, G4: 392.0, A4: 440.0,
  C5: 523.25, D5: 587.33, E5: 659.25, G5: 783.99, A5: 880.0, C6: 1046.5,
};

// Marimba / xilófono: seno + armónicos con decaimiento rápido.
function addMarimba(buf, startSec, freq, amp, dur = 0.9) {
  const s0 = Math.floor(startSec * SR);
  const n = Math.min(Math.floor(dur * SR), buf.length - s0);
  for (let i = 0; i < n; i++) {
    const t = i / SR;
    const env = Math.exp(-t * 8.5);
    const s =
      Math.sin(2 * Math.PI * freq * t) +
      0.35 * Math.sin(2 * Math.PI * freq * 2 * t) * Math.exp(-t * 16) +
      0.1 * Math.sin(2 * Math.PI * freq * 3.01 * t) * Math.exp(-t * 26);
    buf[s0 + i] += amp * env * s;
  }
}

// Bajo pizzicato.
function addBass(buf, startSec, freq, amp, dur = 0.42) {
  const s0 = Math.floor(startSec * SR);
  const n = Math.min(Math.floor(dur * SR), buf.length - s0);
  for (let i = 0; i < n; i++) {
    const t = i / SR;
    const env = Math.exp(-t * 5.5);
    const s = Math.sin(2 * Math.PI * freq * t) + 0.22 * Math.sin(2 * Math.PI * freq * 2 * t) * Math.exp(-t * 12);
    buf[s0 + i] += amp * env * s;
  }
}

// Shaker suave fuera de pulso.
function addShaker(buf, startSec, amp = 0.05) {
  const s0 = Math.floor(startSec * SR);
  const n = Math.min(Math.floor(0.045 * SR), buf.length - s0);
  for (let i = 0; i < n; i++) {
    const t = i / SR;
    buf[s0 + i] += amp * (Math.random() * 2 - 1) * Math.exp(-t * 90);
  }
}

console.log("== BGM: theme_playful_bgm ==");
{
  const BPM = 132;
  const eighth = 60 / BPM / 2;
  const bars = 16;
  const total = bars * 8 * eighth + 0.5;
  const buf = new Float64Array(Math.ceil(total * SR));

  const A = [
    ["C5", null, "E5", null, "G5", null, "E5", null],
    ["D5", null, "G5", null, "A5", null, "G5", null],
    ["E5", null, "C5", null, "G4", null, "C5", null],
    ["D5", null, "E5", null, "D5", null, null, null],
  ];
  const B = [
    ["G5", null, "A5", null, "C6", null, "A5", null],
    ["G5", null, "E5", null, "C5", null, "E5", null],
    ["A5", null, "G5", null, "E5", null, "D5", null],
    ["C5", null, null, null, "G4", null, "C5", null],
  ];
  const song = [...A, ...B, ...A, ...B];
  const chords = [
    [F.C3, F.G3],
    [F.G2, F.D3],
    [F.A2, F.E3],
    [F.F2, F.C3],
  ];

  song.forEach((bar, barIdx) => {
    bar.forEach((note, e) => {
      if (note) addMarimba(buf, (barIdx * 8 + e) * eighth, F[note], 0.42);
    });
    const [root, fifth] = chords[barIdx % 4];
    [0, 2, 4, 6].forEach((e, k) => {
      addBass(buf, (barIdx * 8 + e) * eighth, k % 2 === 0 ? root : fifth, 0.30);
    });
    [1, 3, 5, 7].forEach((e) => addShaker(buf, (barIdx * 8 + e) * eighth));
  });

  writeWav(`${OUT}/theme_playful_bgm.wav`, buf, 0.72, 8, 60);
}

function sweep(buf, startSec, dur, f0, f1, amp, harmonic = 0.15, decay = 14) {
  const s0 = Math.floor(startSec * SR);
  const n = Math.min(Math.floor(dur * SR), buf.length - s0);
  let phase = 0;
  for (let i = 0; i < n; i++) {
    const t = i / SR;
    const u = t / dur;
    const f = f0 + (f1 - f0) * u;
    phase += (2 * Math.PI * f) / SR;
    const env = Math.min(1, t / 0.006) * Math.exp(-t * decay);
    buf[s0 + i] += amp * env * (Math.sin(phase) + harmonic * Math.sin(2 * phase));
  }
}

console.log("== SFX ==");
{
  // ui_success: arpegio de marimba ascendente.
  const buf = new Float64Array(Math.ceil(1.0 * SR));
  addMarimba(buf, 0.0, F.C5, 0.5);
  addMarimba(buf, 0.07, F.E5, 0.5);
  addMarimba(buf, 0.14, F.G5, 0.5);
  addMarimba(buf, 0.21, F.C6, 0.55, 1.2);
  writeWav(`${OUT}/ui_success.wav`, buf, 0.7);
}
{
  // ui_miss: dos notas suaves descendentes.
  const buf = new Float64Array(Math.ceil(0.6 * SR));
  sweep(buf, 0.0, 0.22, F.E4, F.E4 * 0.94, 0.45, 0.1, 8);
  sweep(buf, 0.13, 0.28, F.C4, F.C4 * 0.92, 0.42, 0.1, 7);
  writeWav(`${OUT}/ui_miss.wav`, buf, 0.55);
}
{
  // frog: dos croares graves con caída de tono.
  const buf = new Float64Array(Math.ceil(0.55 * SR));
  const croak = (start) => {
    const s0 = Math.floor(start * SR);
    const dur = 0.17;
    const n = Math.min(Math.floor(dur * SR), buf.length - s0);
    let phase = 0;
    let lp = 0;
    for (let i = 0; i < n; i++) {
      const t = i / SR;
      const f = 185 - 70 * (t / dur);
      phase += (2 * Math.PI * f) / SR;
      const wob = 1 + 0.12 * Math.sin(2 * Math.PI * 32 * t);
      const env = Math.min(1, t / 0.01) * Math.exp(-t * 5.5);
      const raw =
        (Math.sin(phase) + 0.45 * Math.sign(Math.sin(phase)) * 0.6) * wob +
        0.06 * (Math.random() * 2 - 1);
      lp += 0.16 * (raw - lp);
      buf[s0 + i] += 0.55 * env * lp;
    }
  };
  croak(0.02);
  croak(0.25);
  writeWav(`${OUT}/frog.wav`, buf, 0.65);
}
{
  // bird: dos trinos agudos rápidos.
  const buf = new Float64Array(Math.ceil(0.45 * SR));
  sweep(buf, 0.0, 0.13, 1900, 3300, 0.5, 0.12, 16);
  sweep(buf, 0.16, 0.11, 2400, 3700, 0.5, 0.12, 18);
  sweep(buf, 0.16, 0.11, 3600, 2900, 0.18, 0.05, 18);
  writeWav(`${OUT}/bird.wav`, buf, 0.6);
}
{
  // monkey: dos "u-u" con vibrato.
  const buf = new Float64Array(Math.ceil(0.6 * SR));
  const hoot = (start, dur, base) => {
    const s0 = Math.floor(start * SR);
    const n = Math.min(Math.floor(dur * SR), buf.length - s0);
    let phase = 0;
    for (let i = 0; i < n; i++) {
      const t = i / SR;
      const f = base * (1 + 0.06 * Math.sin(2 * Math.PI * 9 * t));
      phase += (2 * Math.PI * f) / SR;
      const env = Math.min(1, t / 0.02) * Math.min(1, (dur - t) / 0.05);
      buf[s0 + i] += 0.5 * env * (Math.sin(phase) + 0.3 * Math.sin(2 * phase) + 0.12 * Math.sin(3 * phase));
    }
  };
  hoot(0.02, 0.18, 400);
  hoot(0.26, 0.24, 330);
  writeWav(`${OUT}/monkey.wav`, buf, 0.6);
}

// Elimina el tema antiguo (sustituido por theme_playful_bgm).
const old = `${OUT}/bgm_theme.wav`;
if (fs.existsSync(old)) {
  fs.unlinkSync(old);
  console.log("removed", old);
}
