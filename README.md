# War Dice 🎲

War Dice is an Android dice-rolling and Monte Carlo simulation tool designed for tabletop wargames (e.g. Warhammer 40,000).  

It supports simple dice rolls, full hit–wound–save sequences, and large simulations with charts and cumulative probability readouts.

---

## Features

### 1. Simple Roll (Single Screen)
- Choose:
  - **Number of dice**
  - **Die type**: `d2`, `d3`, `d4`, `d6`, `d8`, `d10`, `d12`, `d20`, `d100`
- See:
  - Individual roll results
  - Summary stats: total, average, count per face
- (Optional in future) quick presets for common pools (e.g. 10×d6, 20×d6).

> This is the basic “calculator” mode for quick one-off dice pools.

---

### 2. Sequence Roll (Warhammer-style Hit → Wound → Save)

The **Sequence Roll** screen models a full attack sequence in 40k-style systems:

**Configurable inputs**

- **Attacks**
  - Flat attacks: `Attacks` (e.g. 24)
  - **Extra attacks**: flat number added before hit rolls
  - Optional **attacks as dice**:
    - `XdY` attacks (e.g. `3d6` attacks)
- **Die type** (e.g. d6)
- **Hit / Wound / Save thresholds**: `X+`
- **Critical thresholds**
  - Critical Hits ≥ `N`
  - Critical Wounds ≥ `N`

**Reroll options**

Hits:
- Reroll failed hit rolls
- Reroll hit rolls of 1
- Reroll **all** hit rolls **except criticals**
- Reroll **successful** hit rolls

Wounds:
- Reroll failed wound rolls (Twin-linked)
- Reroll wound rolls of 1
- Reroll all wound rolls except criticals
- Reroll successful wound rolls

**Special rules**

- **Sustained Hits X** – each critical hit generates **X** additional hits  
- **Lethal Hits** – critical hits become **auto-wounds**
- **Devastating Wounds** – critical wounds become **unsavable** (bypass saves)
- **Torrent** – attacks become **auto-hits** (skip hit roll)

All of the above are encoded in a `ThresholdRollConfig` and processed by the dice engine.

---

### 3. Simulation Mode

For any Sequence Roll configuration you can enable **Simulating**:

- Input **number of runs** (e.g. `1 000 000`)
- The app runs the full sequence that many times using the dice engine
- Produces distributions for:
  - Hits
  - Wounds
  - Unsaved wounds

**Charts**

- Uses **MPAndroidChart** (`BarChart`) to show:
  - Histogram of `P(X = n)` for each count
- Charts are placed under **three tabs**:
  - **Hits**
  - **Wounds**
  - **Unsaved**
- Each tab has:
  - A bar chart with a dark/grey background for readability in dark mode
  - A text block listing cumulative percentages

**Cumulative probability text**

For each metric (Hits, Wounds, Unsaved) the app computes cumulative probabilities:

- `P(X ≥ n)` for each `n`
- Only shows “interesting” lines where:

  ```text
  5% < P(X ≥ n) < 95%
