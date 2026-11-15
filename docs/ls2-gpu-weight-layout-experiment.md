# Pixel 7 Pro / Tensor G2 — real LS2 GPU weight-layout experiment

This branch tests **the original full Light Speed 2 ONNX model converted by Jetlink**, not a synthetic matmul. Its predecessor's real on-device profiler found ~81% of reported GPU operator time among 1x1 convolution kernels. This does not prove those kernels can be accelerated by 4x, but identifies a useful weight-storage experiment.

## What changes

LiteRT 2.2 fp16 OpenCL is unchanged unless the user selects an experiment under **Settings → Performance → GPU experiment**:

| Experiment | GPU TOML addition | Hypothesis |
| --- | --- | --- |
| Stock GPU (baseline) | no addition | Original measured ~229.7 ms/frame (Pixel 7 Pro at nominal temp) |
| Texture weights | `prefer_texture_weights = true` | Fixed convolution weights may read more efficiently in texture storage |
| Buffer storage | `buffer_storage_type = 1` | Force buffer layout instead of GPU auto-selected storage |
| High GPU priority | `priority = 3` | See whether queue priority changes measured runtime |

All variants preserve `precision = 1` (fp16), `backend = 1` (OpenCL) and the source ONNX/TFLite file. The original stock OpenCL compiled-program cache key is unchanged; experimental modes salt the key with their mode. They share the **same prepared model artifact**, so on switching modes Jetlink restarts the server and recompiles/caches GPU programs as needed without repeating ONNX download or conversion.

**For OFFROAD profiling only. Do not connect to a comma while testing; these changes have not been road-validated for numerical equivalence or end-to-end timing.** If a mode fails to prepare or load, select Stock GPU again.

## Test on the Pixel 7 Pro

1. Install the APK produced by GitHub Actions. APK signing may differ from the prior app; if Android requires uninstalling the existing app, that deletes its private cache and means reacquiring LS2 *once*.
2. Select **Processor → GPU**, then **GPU experiment → Stock GPU**. Make sure the model is ready and no comma is connected.
3. Run **Benchmark → 1 Minute**; share the report. Its third line should say **GPU tuning: default**. We expect roughly 230 ms average when nominal.
4. Return to **Settings → Performance → GPU experiment → Texture weights**. The service restarts and the *same prepared model* should be used. Wait for Ready, run **Benchmark → 1 Minute**, share.
5. Test **Buffer storage** in the same manner. **High GPU priority** is optional.
6. Keep the phone at similar temperature and power conditions; discard a trial if its thermal status changes. Reverse the order or repeat Stock at the end if differences are small (<5%).
7. Restore Stock GPU after testing. No optimisation should be used for driving until output parity is tested with reference input tensors.

A meaningful result should demonstrate repeatable reductions in *accelerator mean/P90/P99* without increased error or instability. This app does **not** use the 5.7-second profiling configuration; it is built from the stock main branch.

## Baseline vs target

Real LS2 baseline at nominal temp: mean 229.7ms, accelerator 229.5ms, P99 235.7ms. Goal <50 ms/frame requires ~4.6x. These option changes are a narrow first step, not a promise of 4.6x improvement. If they don't meaningfully improve latency, next engineering effort should evaluate a materially different GPU kernel implementation or selective model refactor and verify prediction accuracy, not keep toggling flags indefinitely.
