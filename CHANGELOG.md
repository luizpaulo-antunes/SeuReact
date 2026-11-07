# Changelog

Todas as mudanças relevantes deste projeto são documentadas aqui. O projeto segue [Semantic Versioning](https://semver.org/lang/pt-BR/) e mantém uma seção `Não lançado` para o próximo ciclo.

## Não lançado

### Planejado

- Executar a matriz completa de validação em aparelhos físicos.
- Refinar sincronização por modelo de aparelho e codecs HDR.

## 0.2.0 - 2026-11-07

### Adicionado

- Máscaras circular, quadrada e arredondada aplicadas à câmera no MP4 por shader OpenGL.
- Zoom de `1x` a `4x` na fonte, registrado na timeline e reproduzido na exportação.
- Apontadores temporários de dedo, seta e mouse para destacar regiões do vídeo.
- Migração Room `1 → 2` para persistir os novos eventos sem perder projetos existentes.

### Corrigido

- A opção de proporção `Original` agora respeita a rotação codificada do vídeo e preserva fontes verticais.
- Controles de gravação reorganizados para facilitar o uso com uma mão.

### Qualidade

- Vinte e um testes unitários, Android Lint, APK debug e APK de testes instrumentados validados localmente.

## 0.1.3 - 2026-11-07

### Corrigido

- A câmera gravada agora é desenhada acima do vídeo original no MP4 exportado.
- Ordem das entradas do Media3 alinhada à composição reversa do `DefaultVideoCompositor`.
- Resolução na proporção original passa a usar os metadados da fonte, independentemente da entrada primária.

### Qualidade

- Dois testes de regressão garantem a ordem câmera-frente e vídeo-fundo.
- Quinze testes unitários, Android Lint e APK debug validados localmente.

## 0.1.2 - 2026-11-07

### Corrigido

- Exportação não falha mais ao preparar a matriz de áudio `7 → 2` do Media3.
- Downmix multicanal agora possui fallback estéreo próprio com potência normalizada.
- Canais centrais, LFE e surround são preservados no fallback sem exceder a potência de saída.

### Qualidade

- Três testes de regressão para matriz de sete canais, normalização e canal surround central.
- Treze testes unitários, Android Lint e APK debug validados localmente.

## 0.1.1 - 2026-11-07

### Corrigido

- Solicitação das permissões de câmera e microfone antes do vínculo do preview CameraX.
- Exibição inicial da câmera frontal, com fallback seguro quando ela não estiver disponível.
- Finalização de câmera e microfone sem deixar o projeto permanentemente em `Gravando`.
- Tratamento de erros e timeout de finalização, preservando o material bruto como projeto interrompido.

### Qualidade

- Três testes de regressão para sucesso, erro da câmera e interrupção pelo ciclo de vida.
- Testes unitários, Android Lint e APK debug validados localmente.

## 0.1.0 - 2026-11-07

### Adicionado

- Importação e validação de vídeos locais com cópia privada recuperável.
- Reprodução da fonte com Media3 e preview frontal com CameraX.
- Gravação contínua de câmera e microfone durante pausas e seeks.
- Timeline monotônica com play, pause, seek, mute, volumes e transformações.
- Compilação da timeline em segmentos reproduzíveis e frames congelados.
- Exportação H.264/AAC com picture-in-picture, mixagem estéreo e MediaStore.
- Persistência Room, recuperação de projetos interrompidos e WorkManager foreground.
- Interface Compose para início, preparação, gravação, progresso e compartilhamento.
- Testes unitários da timeline e testes instrumentados de recuperação.
- Gradle Wrapper, Android Lint e documentação de build e validação física.
