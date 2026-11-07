# Seu React

Aplicativo Android nativo, offline, para gravar uma reação contínua enquanto um vídeo local é reproduzido, pausado ou reposicionado. O resultado é exportado como MP4 para `Filmes/SeuReact` sem capturar a interface do aplicativo.

## Projeto

- [Changelog](CHANGELOG.md)
- [Roadmap](docs/ROADMAP.md)
- [Processo de releases](docs/RELEASES.md)

## Arquitetura

- `ui`: Compose + Material 3, estado MVVM e controles da sessão.
- `domain`: projetos, eventos monotônicos e `TimelineCompiler` puro.
- `data`: Room, persistência incremental e recuperação de gravações interrompidas.
- `media/importer`: cópia privada e validação de trilhas com `MediaExtractor`.
- `media/capture`: CameraX grava câmera e microfone continuamente.
- `media/export`: Media3 Transformer usa MediaCodec/OpenGL/MediaMuxer internamente, reconstrói a fonte por segmentos, mistura áudio PCM com headroom e compõe picture-in-picture.
- `worker`: exportação cancelável em WorkManager com progresso e notificação foreground.

O MP4 bruto do CameraX contém vídeo da câmera e áudio do microfone em trilhas separadas. O áudio original nunca é recapturado pelo microfone; ele é lido diretamente do arquivo importado. Use fones durante a sessão para minimizar vazamento acústico.

## Timeline

Todo evento armazena o tempo final relativo a `elapsedRealtimeNanos` e a posição observada da fonte. O compilador gera segmentos com dois relógios independentes:

- timeline final/câmera: sempre avança;
- timeline da fonte: avança durante play, permanece fixa durante pause e muda imediatamente em seek.

Pausas são exportadas como imagens com duração; seeks e repetições viram clipes consecutivos do arquivo original. Transformações, mute, volumes, zoom e apontadores são associados ao segmento vigente.

Durante a gravação, a fonte aceita zoom de `1x` a `4x` e toque para destacar um ponto com dedo, seta ou mouse. Preview e MP4 usam os mesmos vetores e hotspots; na seta, a ponta termina exatamente no local tocado e a cauda se adapta às bordas. Esses efeitos pertencem à composição e aparecem no MP4 sem incluir os controles da interface. A sessão pode ser finalizada para exportar ou descartada, após confirmação, sem gerar um vídeo.

## Requisitos

- Android Studio com JDK 17.
- Android SDK 36 e Build Tools correspondentes.
- Aparelho físico Android 10 (API 29) ou superior, com câmera e microfone.

O Gradle Wrapper 8.11.1 está incluído no repositório.

## Compilar e instalar

1. Abra a raiz no Android Studio e aguarde o sync.
2. Se necessário, defina `sdk.dir=/caminho/do/Android/sdk` em `local.properties`.
3. Execute `./gradlew assembleDebug`.
4. Conecte o aparelho com depuração USB e execute `./gradlew installDebug`.

O app solicita apenas câmera, microfone e, no Android 13+, notificações. A seleção usa Photo Picker; salvar usa MediaStore e não requer permissão ampla de armazenamento.

## Testes

```bash
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
```

Os testes unitários cobrem conversão de eventos, pausa longa, seeks nos dois sentidos, repetição, mute, volume, transformações e duração final. Os testes instrumentados cobrem recuperação de projetos e preservação dos arquivos brutos após falha.

## Validação em aparelho físico

Use vídeos curtos antes dos cenários longos e confira duração, sincronia labial e pontos de seek no MP4 da galeria.

| Cenário | Verificação |
|---|---|
| MP4 vertical e horizontal | letterbox/crop e orientação corretos |
| Formato Original | mesma proporção visual da fonte, incluindo rotação 90°/270° |
| Câmera circular/arredondada | máscara correta no preview e no MP4 |
| Zoom e apontadores | enquadramento e destaque coincidem com a gravação |
| Descartar gravação | câmera finaliza com segurança; projeto é removido e nenhuma exportação inicia |
| Pausa de 30 segundos | fonte congelada; câmera e microfone contínuos |
| Muitos seeks | saltos e repetições na ordem realizada |
| Fonte sem áudio | comentário presente e sem falha na composição |
| Cancelar exportação | WorkManager cancela; brutos permanecem no projeto |
| Pouco espaço | erro compreensível; fonte e câmera não são removidas |
| Perda de primeiro plano | projeto marcado como interrompido e recuperável |
| Interrupção de câmera/mic | sessão finalizada com segurança e mensagem de erro |

## Limitações e riscos conhecidos

- O tempo inicial da sessão é ancorado no evento `VideoRecordEvent.Start`; aparelhos podem introduzir pequena latência interna antes do primeiro sample. Valide sincronia por modelo.
- Seek depende da precisão do decoder e dos keyframes do arquivo; Transformer decodifica a partir do ponto seguro e recorta por timestamp.
- O compositor Media3 é uma API marcada como instável; está isolado atrás de `ReactionExporter` para permitir troca por um pipeline MediaCodec/OpenGL próprio.
- O crop da câmera usa `Presentation` do Media3; a máscara e a composição ainda usam efeitos OpenGL. Valide o pipeline nos aparelhos e GPUs que representam o público-alvo.
- Arquivos HDR são tratados pelo comportamento padrão do Transformer; a matriz de validação deve incluir aparelhos e codecs usados pelo público-alvo.

## Privacidade e arquivos

Nenhum dado sai do aparelho. Fontes importadas e brutos ficam em `files/projects/<id>` e não são apagados automaticamente em falhas. O MP4 final é publicado via MediaStore somente depois que a escrita termina.
