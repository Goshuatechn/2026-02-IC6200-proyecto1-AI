# 2026-02-IC6200-proyecto1-AI
Reconocimiento de comandos de voz con PyTorch (LeNet-5 y arquitectura alternativa) entrenado con Speech Commands v0.02, exportado a ONNX e integrado en una app Android de navegación Smart Home con ONNX Runtime Mobile. Proyecto I – Inteligencia Artificial, TEC.


# Proyecto I – Reconocimiento de comandos de voz (Smart Home Voice Navigator)

## Estructura
- `/ia` → Notebook, entrenamiento, modelos y export ONNX
- `/app` → App Android (Kotlin) con ONNX Runtime Mobile
- `/informe` → Fuentes LaTeX y PDF

## Contrato del modelo (no cambiar sin avisar)
- Entrada: audio mono, 16 kHz, 1 segundo → tensor float32 de 16000 valores
- Preprocesamiento: espectrograma calculado dentro del modelo (MelSpectrogram, opset 18)
- Salida: 10 logits en este orden:
  `yes, no, up, down, left, right, on, off, stop, go`
- App: ignorar predicciones con confianza < 70 %
