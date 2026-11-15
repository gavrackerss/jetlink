# Pixel 11 / Tensor G6 stock-phone NNAPI test

This diagnostic asks whether Android's public NNAPI route can compile and execute
the entire Light Speed 2 graph on the stock phone. It does not change or bypass
the application authorization policy of the direct Google Tensor LiteRT compiler.

The separate package `dev.tensorprobe.g6nnapi` installs beside normal Jetlink.
On Tensor G6, the only processor choice is `Full model · Tensor NNAPI`, selected
by default. USB and remote frame serving are disabled for this backend.

Before starting the server, a small NDK library enumerates public NNAPI devices.
The check proceeds only when every non-CPU device is named `google-edgetpu` and
has accelerator type 4. An absent accelerator, a GPU or an unidentified device
stops the test and appears in Logs. No model is downloaded by this preflight.
This conservative check identifies the eligible driver; it does not prove that
every operation inside the vendor driver executes on physical NPU hardware.

The existing unchanged Swift/C backend uses ONNX Runtime 1.29.0 with
`NNAPI_FLAG_USE_FP16 | NNAPI_FLAG_CPU_DISABLED` and
`session.disable_cpu_ep_fallback=1`. The full original ONNX graph is used, not a
single-layer slice or a cached precompiled Google model. The build reuses a
hash-pinned native artifact from source commit
`c740ba36e6cef705baee51244bde1372be575d08` and verifies those sources are unchanged.

## Run

1. Open **Jetlink G6 NNAPI Test** on the Pixel 11, disconnected from the comma.
2. If startup reports an NNAPI failure, share **Logs**. Do not download a model.
3. Otherwise open **Models**, refresh if needed, and choose **Light Speed 2**
   (4 November 2026). Use/Get downloads and prepares it. The original ONNX SHA256
   must be `909a140019050a9d7f00db22dd4f4085928d2af9474a82244672ddab964f95a8`.
   Allow roughly 0.77 GB for the download and several GB free for preparation.
4. If preparation fails, share **Logs**. The failure is a result; no CPU/GPU
   substitute is accepted as a successful run.
5. Only if the model becomes Ready, run **Benchmark → 1 Minute**, then share its
   report and Logs. A later 10-minute run can assess sustained timing.

The benchmark uses generated camera inputs and carries recurrent state between
frames. It measures the phone's model pipeline, excluding the actual vehicle
link. Numerical parity on real captured inputs is not checked by this build.
Model load, a fast average or even a completed one-minute benchmark is not
validation for driving or proof of sustained end-to-end 20 FPS.

## Interpretation

- Inventory refusal: this NNAPI backend cannot isolate a known Tensor driver.
- ORT/NNAPI compilation failure: this particular full-model route is unsupported;
  it does not prove every possible conversion or compiler route is impossible.
- Completed frames: evidence of full-model execution through the selected NNAPI
  route; examine timing, outputs and longer runs separately.

No Pixel 11 device result was available when this build was produced.
