# Roadmap

O roadmap é organizado em releases pequenas e verificáveis. Datas são definidas quando uma versão entra em desenvolvimento.

## 0.2.0 — Composição visual

- Máscaras circular, quadrada e retângulo arredondado no MP4 final.
- Crop e letterbox validados para original, 9:16, 16:9 e 1:1.
- Reposicionamento e escala com limites adaptados à proporção de saída.
- Testes de exportação com vídeos verticais e horizontais.

## 0.3.0 — Robustez de mídia

- Calibração de offset inicial entre CameraX e relógio monotônico.
- Matriz de codecs, vídeos sem áudio, HDR e taxas de amostragem variadas.
- Tratamento dedicado para pouco espaço e falhas de encoder.
- Métricas locais de duração e drift audiovisual para diagnóstico.

## 0.4.0 — Projetos e recuperação

- Detalhes, renomeação e descarte seguro de projetos.
- Recuperação guiada após interrupção da câmera, microfone ou processo.
- Limpeza explícita de arquivos temporários sem remover material bruto útil.
- Histórico e reexecução de exportações.

## 1.0.0 — MVP validado

- Critérios de aceite executados em aparelhos Android 10 a Android 16.
- Sincronização audiovisual validada em sessões curtas e longas.
- Máscaras, proporções, cancelamento e compartilhamento aprovados.
- Documentação de suporte, privacidade e limitações consolidada.

## Fora do roadmap

Gravação de tela, overlays sobre outros aplicativos, downloads de redes sociais, streaming, backend e publicação automática continuam fora do escopo.
