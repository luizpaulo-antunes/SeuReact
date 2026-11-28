# Changelog

Todas as mudanças relevantes deste projeto são documentadas aqui. O projeto segue [Semantic Versioning](https://semver.org/lang/pt-BR/) e mantém uma seção `Não lançado` para o próximo ciclo.

## Não lançado

## 0.3.0-rc.3 - 2026-11-28

### Corrigido

- Os streams das câmeras frontal e traseira agora recebem o mesmo crop central em `9:16` antes da composição, evitando que buffers alternativos do modo concorrente sejam achatados dentro da janela menor.
- A prévia DualCam usa o mesmo preenchimento central da gravação para manter o enquadramento exibido e o MP4 alinhados.

### Qualidade

- Teste de regressão fixa a proporção do viewport no mesmo formato vertical do canvas de gravação.
- Release candidate destinada à nova validação física da câmera menor com ambas as lentes.

## 0.3.0-rc.2 - 2026-11-28

### Corrigido

- A janela menor do DualCam agora usa escala uniforme nos dois eixos, preservando a proporção da câmera sem achatar rostos ou objetos.
- A área de arraste e os limites da janela menor acompanham a geometria real exibida na prévia e gravada no MP4.

### Qualidade

- Teste de regressão garante que a composição Picture-in-Picture mantenha a mesma escala horizontal e vertical.
- Release candidate destinada à validação física do enquadramento da câmera menor em aparelhos com câmeras concorrentes.

## 0.3.0-rc.1 - 2026-11-26

### Adicionado

- Modo DualCam independente, com câmera traseira em tela cheia e frontal circular gravadas simultaneamente no mesmo MP4.
- Janela menor do DualCam arrastável e redimensionável, com troca ao vivo entre a câmera principal e a câmera em destaque.
- Salvamento direto da gravação DualCam em `Filmes/SeuReact`, seguido por confirmação e ação para assistir ao resultado.
- Troca entre câmera frontal e traseira durante a gravação, preservando um único arquivo de câmera e áudio contínuo.
- Estúdio automático em paisagem para fontes e saídas horizontais, com controles em painel lateral.
- Canvas de preparação na proporção real da saída, mantendo a câmera arrastável e redimensionável nos formatos circular, quadrado e arredondado.

### Interface

- Nova entrada `Gravar com DualCam` na Home, com fluxo próprio de permissão, preparação, gravação, finalização e resultado.
- Estados dedicados para aparelho incompatível, permissão negada, preparação, gravação, salvamento e erro recuperável.

### Corrigido

- Espelhamento da câmera agora acompanha cada troca de lente na timeline e no MP4 exportado.
- Posição e tamanho do overlay no preview agora correspondem ao resultado exportado, inclusive em `16:9`.
- Orientação fica travada durante gravação e finalização para evitar interrupção por rotação do aparelho.

### Validação pendente

- Validar em aparelho físico a composição concorrente, espelhamento, troca principal/PiP e o MP4 salvo pelo CameraX 1.7.
- Executar a matriz completa de validação de reações horizontais em aparelhos físicos.
- Refinar sincronização por modelo de aparelho e codecs HDR.

### Qualidade

- Quarenta e cinco testes unitários aprovados, incluindo geometria do DualCam, orientação e troca de lente na timeline.
- Android Lint aprovado sem erros ou avisos.
- APK debug e APK de testes instrumentados compilados localmente.

## 0.2.2-rc.5 - 2026-11-07

### Interface

- Home redesenhada como uma galeria responsiva, com cards visuais em duas colunas, duração, orientação e estado de cada projeto.
- Novo destaque para retomar projetos incompletos, falhos ou prontos para exportar sem procurar na galeria.
- Ação `Criar novo react` ganhou hierarquia principal e orientação sobre o uso de fones integrada ao cabeçalho.
- Estúdio recebeu cabeçalho compacto com nome do vídeo e indicador `REC` mais legível.
- Preparação e exportação foram reorganizadas em superfícies claras, com estados, progresso e próximas ações visualmente separados.
- Paleta, tipografia, cantos e espaçamentos foram unificados em um tema Material 3 próprio do SeuReact.

### Escopo

- Esta candidata altera apenas a apresentação em Jetpack Compose; captura, timeline, composição, áudio e exportação permanecem idênticos à `rc.4`.

### Qualidade

- Trinta e nove testes unitários aprovados.
- Android Lint, APK debug e APK de testes instrumentados validados localmente.

## 0.2.2-rc.4 - 2026-11-07

### Corrigido

- O vídeo original agora é escalado dos pixels nativos para preencher a saída mantendo sua proporção, eliminando a miniatura central cercada por preto.
- A câmera volta a permanecer visualmente vinculada ao vídeo, preservando posição e tamanho relativos no preview e no MP4.

### Interface

- Painel de gravação reduzido para manter o vídeo como elemento principal da tela.
- Timeline, play/pause, saltos, mute, descarte e finalização permanecem sempre visíveis.
- Marcadores, zoom e volume ficam recolhidos em `Destaques e ajustes` e só ocupam espaço quando solicitados.
- As opções de marcador agora são `Toque`, `Seta` e `Clique`.
- `Clique` usa um alvo pulsante de alto contraste em vez de um desenho de mouse.

### Qualidade

- Trinta e nove testes unitários aprovados, incluindo escala do vídeo original em resoluções e proporções diferentes.
- Android Lint, APK debug e APK de testes instrumentados validados localmente.

## 0.2.2-rc.3 - 2026-11-07

### Corrigido

- Corrigida a falha `IllegalStateException: Tamanho da câmera ainda não configurado` reportada pela `rc.2`.
- O compositor não depende mais de `getOutputSize` ser chamado antes de `getOverlaySettings`, pois Media3 não garante essa ordem.
- Dimensões orientadas da câmera e da saída agora são calculadas antes de iniciar o Transformer e permanecem disponíveis desde a primeira solicitação de overlay.

### Qualidade

- Trinta e sete testes unitários aprovados, incluindo regressão que calcula o overlay da câmera antes da solicitação do tamanho de saída.
- Android Lint, APK debug e APK de testes instrumentados validados localmente.

## 0.2.2-rc.2 - 2026-11-07

### Corrigido

- Removido `Presentation` da sequência da câmera após a validação física mostrar que a mudança de geometria ainda causava `ERROR_CODE_VIDEO_FRAME_PROCESSING_FAILED`.
- O frame da câmera mantém as mesmas dimensões do decoder durante todo o pipeline; o crop central `1:1` ou `4:3` ocorre apenas nas coordenadas de textura do shader.
- A máscara e o crop passam pelo mesmo efeito OpenGL, evitando renegociação de tamanho entre segmentos e mantendo círculo, quadrado e retângulo sem esticar a imagem.
- Mensagens de falha agora incluem a causa técnica OpenGL/codec mais profunda em uma única linha para diagnóstico no aparelho.

### Qualidade

- Trinta e seis testes unitários aprovados, incluindo crop de câmeras verticais e horizontais sem alteração das dimensões do frame.
- Android Lint, APK debug e APK de testes instrumentados validados localmente.

## 0.2.2-rc.1 - 2026-11-07

### Corrigido

- Regressão da `0.2.1` que causava `Video frame processing error` em todas as exportações.
- Crop da câmera transferido para o efeito oficial `Presentation` do Media3; o shader de máscara preserva as dimensões recebidas.
- Círculo e quadrado usam crop central `1:1`, enquanto o retângulo arredondado usa `4:3`, sem esticar a imagem.
- Seta redesenhada como callout azul e branco, sem círculo ou amarelo, com a ponta exatamente no local tocado e orientação automática nas bordas.
- Preview e MP4 agora compartilham os mesmos vetores, geometria, cores e hotspot para dedo, seta e mouse.

### Recuperação

- Erros de exportação agora diferenciam processamento de frame, decoder, encoder, áudio, muxer e entrada/saída, com código técnico curto.
- A cadeia completa da falha é registrada no log e os arquivos brutos continuam preservados para nova tentativa.
- Projetos que falharam na `0.2.1` podem ser reexportados nesta versão sem migração de dados.

### Qualidade

- Trinta e três testes unitários aprovados, incluindo regressões de dimensões da máscara, proporções e ordem dos efeitos, hotspot e inversão da seta e mensagens de exportação.
- Android Lint, APK debug e APK de testes instrumentados validados localmente.
- Release candidate destinada à validação no mesmo aparelho antes da promoção para `0.2.2` estável.

## 0.2.1 - 2026-11-07

### Corrigido

- Câmera circular e quadrada agora usam crop central e escala baseada nos pixels reais da entrada e saída, evitando máscaras ovais.
- Retângulo arredondado usa crop `4:3` sem distorcer a imagem da câmera.
- Apontadores de dedo, seta e mouse foram redesenhados sem os traços amarelos e ganharam destaque pulsante de alto contraste.
- Ajustar o volume não reativa o áudio original enquanto o mute estiver ligado.
- Falhas anteriores ao primeiro frame não geram mais timelines incompletas.

### Adicionado

- Botão `Descartar` durante a gravação, com confirmação explícita, finalização segura da câmera e nenhuma exportação.
- Remoção do projeto e dos arquivos da sessão somente após o usuário confirmar o descarte.

### Qualidade

- Auditoria dos fluxos de importação, preparação, captura, timeline, exportação, cancelamento e recuperação.
- Vinte e seis testes unitários, Android Lint, APK debug e APK de testes instrumentados validados localmente.

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
