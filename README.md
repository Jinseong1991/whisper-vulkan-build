# whisper-vulkan-build

공식 [ggml-org/whisper.cpp](https://github.com/ggml-org/whisper.cpp) 의 `whisper-server` 를 **Windows Vulkan 판** 으로 빌드해 올리는 저장소입니다.
(VRC Live Translator 의 AMD·인텔 그래픽카드 지원용. 공식 릴리스에는 Vulkan 판이 없어서 직접 빌드합니다.)

## 출처

- 소스: <https://github.com/ggml-org/whisper.cpp/tree/v1.9.5> (태그 `v1.9.5`, 커밋 `d1be6fde11ac6e0407606b4e42fe72d34add8037`). 소스는 고치지 않고 워크플로가 커밋을 확인합니다.
- Vulkan SDK 1.4.357.0 (LunarG 공식 내려받기, 워크플로가 SHA-256 을 확인).
- 빌드는 `.github/workflows/build.yml` (GitHub Actions, `windows-2022`)에서만 합니다. 쓰는 외부 액션은 커밋 SHA 로 고정했습니다.

## 빌드 플래그

```
cmake -B build -DGGML_VULKAN=ON -DWHISPER_BUILD_SERVER=ON -DBUILD_SHARED_LIBS=ON -DGGML_NATIVE=OFF \
      -DGGML_BACKEND_DL=ON -DGGML_CPU_ALL_VARIANTS=ON -DWHISPER_SDL2=OFF
cmake --build build --config Release
```

- `GGML_NATIVE=OFF` + `GGML_BACKEND_DL=ON` + `GGML_CPU_ALL_VARIANTS=ON`: 빌드 서버 CPU 에만 맞춘 명령어를 쓰지 않고, CPU 명령어별 `ggml-cpu-*.dll` 변형을 여러 개 넣어 실행할 때 고릅니다 (어느 CPU 에서나 돌게).
- `GGML_VULKAN=ON`: `ggml-vulkan.dll` (AMD·인텔·NVIDIA 의 Vulkan 1.2 이상 드라이버).
- `WHISPER_SDL2=OFF`: SDL2 를 넣지 않습니다.

## 릴리스

태그 `whisper-v<버전>-vulkan-<번호>` 릴리스에 `whisper-v1.9.5-win-vulkan-x64.zip` 과 `.sha256` 이 있고, 릴리스 본문에 소스 커밋·플래그·SHA-256 이 적혀 있습니다.
zip 안: `whisper-server.exe`, `ggml*.dll`, `whisper.dll`, `ggml-cpu-*.dll` 변형들, `LICENSE-whisper.cpp.txt`.

## 라이선스

whisper.cpp 는 MIT 라이선스입니다 (Copyright (c) 2023-2026 The ggml authors). 이 저장소의 `LICENSE-whisper.cpp.txt` 는 그 원문이고 워크플로·README 도 같은 MIT 조건으로 둡니다. zip 안에 whisper.cpp 의 LICENSE 를 함께 넣었습니다.
