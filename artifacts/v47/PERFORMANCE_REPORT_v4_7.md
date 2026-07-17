# v4.7 performance report

Device: OnePlus CPH2691, Android 15. Scenario: 100 identical warm primary-tab switches.

| Measurement | Initial v4.7 pass | Final cached-page `GONE` pass |
|---|---:|---:|
| P50 | 20 ms | 20 ms |
| P90 | 27 ms | 26 ms |
| P95 | 29 ms | 27 ms |
| P99 | 38 ms | 31 ms |
| Maximum | 57 ms | 46 ms |
| Frames >50 ms | 1 | 0 |
| Frames >100 ms | 0 | 0 |
| PSS delta | +122 KB | +645 KB |

Final PSS was 63,698 KB before and 64,343 KB after 100 switches. The requested P50 target (16 ms) was not reached. P95, P99, long-frame, and PSS targets were reached.

Trace files:

- `v47_after.perfetto-trace`
- `v47_after_gone.perfetto-trace`

The comparison uses the same device and the same 100-switch scenario. No claim is made that the P50 target passed.

