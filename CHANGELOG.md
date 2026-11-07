# Changelog

Todas as mudanças relevantes deste projeto são documentadas aqui. O projeto segue [Semantic Versioning](https://semver.org/lang/pt-BR/) e mantém uma seção `Não lançado` para o próximo ciclo.

## Não lançado

### Planejado

- Aplicar máscaras circular e arredondada no compositor exportado.
- Executar a matriz completa de validação em aparelhos físicos.
- Refinar sincronização por modelo de aparelho e codecs HDR.

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
