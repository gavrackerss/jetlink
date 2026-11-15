# Pixel 7 Pro — ONE full-model 20 FPS go/no-go test

This branch is **not** another small graph or extrapolation. It adds a separate processor choice, **G2 full model · strict NNAPI**, to Jetlink on Pixel 7 Pro (GS201 / Tensor G2). The original full Light Speed 2 source ONNX is prepared using Jetlink's ordinary *plain* whole-graph path. No ReLU substitution, tensor slicing, downscaled FC layers, or synthetic replacement graph is used.

## Safeguards and critical limits

- Android's standard ONNX Runtime **NNAPI** execution provider is substituted for the separate Snapdragon-QNN AAR **in this experimental build only**.
- Preflight: the runtime must advertise NNAPIExecutionProvider and export the NNAPI C registration API, or the test reports COMPATIBILITY FAIL **before copying the ~769MB model**.
- Session uses both **NNAPI_FLAG_CPU_DISABLED** and **session.disable_cpu_ep_fallback=1**. It is a hard error if any graph node would be assigned to the default CPU execution provider or if the NNAPI provider can't accept the original full model.
- **NNAPI can use a GPU as well as a Tensor NPU.** These flags alone do not establish exclusive google-edgetpu execution. Therefore a passing run would establish the 20 FPS goal for the *phone's NNAPI accelerator path* (not NPU exclusivity). The previous native small tests established google-edgetpu exclusivity only for those smaller layers.
- **No prediction parity against real driving data** is performed. The full Jetlink benchmark uses random camera bytes and zeroed scalars with recurrent state fed back. Outputs must be finite. This is an engineering performance gate only, not road validation.
- The app refuses to benchmark while the comma is connected. **Keep this build offroad.**
- The branch leaves production main and the earlier working Jetlink unchanged.

## One-shot procedure

1. Sideload the APK produced by https://github.com/gavrackerss/tensortest/actions/workflows/build-jetlink-g2-fullmodel-20fps-gate.yml.
2. Because GitHub Actions creates a new debug signing certificate, Android may require uninstalling the earlier Jetlink. That clears the private downloaded model cache; download LS2 **once** in this build, if necessary.
3. Disconnect the comma. Open **Settings → Performance → Processor → G2 full model · strict NNAPI**.
4. Open Models and select/prepare the downloaded **Light Speed 2**, SHA256 909a140019050a9d7f00db22dd4f4085928d2af9474a82244672ddab964f95a8.
5. **If compilation fails**, copy/share the engine failure message. That is **COMPATIBILITY FAIL** for full-model no-CPU-fallback NNAPI. No other benchmark required.
6. **If and only if it loads Ready**, run **Benchmark → 1 Minute**. Tap Share. Read the **FULL-MODEL 20 FPS GATE: PASS/FAIL** line.

Pass requires a completed ~60 second run, at least 1150 measured frames (after warmup), frame P99 <= 50ms, **zero** frames >50ms, and no serious/critical thermal state at completion.

A compatibility failure means the unmodified full model is **not usable via this NNAPI-only software route**, even though a small quantized FC graph worked on the TPU. It does **not** prove the TPU is fundamentally unable to execute a rearchitected/quantized model, and it does **not** prove a different device would fail.

No further synthetic tests or tuning variants are part of this decision. The known full-model stock GPU baseline is ~230.5ms/frame, well above 50ms.
