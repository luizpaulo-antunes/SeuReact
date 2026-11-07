# Processo de releases

## Versionamento

O projeto usa Semantic Versioning:

- `MAJOR`: mudanças incompatíveis ou marco de produto.
- `MINOR`: novas capacidades compatíveis.
- `PATCH`: correções sem mudança funcional incompatível.

Durante o desenvolvimento inicial, releases `0.x` representam marcos funcionais. A branch principal é `main` e deve permanecer compilável.

## Fluxo

1. Criar uma branch curta a partir de `main`, como `feature/export-mask` ou `fix/audio-drift`.
2. Implementar com testes proporcionais ao risco.
3. Atualizar a seção `Não lançado` em `CHANGELOG.md`.
4. Abrir pull request usando o template do repositório.
5. Confirmar testes unitários, Lint e build debug no CI.
6. Mesclar na `main` após revisão.
7. Mover mudanças de `Não lançado` para a nova versão e data.
8. Atualizar `versionCode` e `versionName` em `app/build.gradle.kts`.
9. Criar tag anotada `vX.Y.Z` e uma GitHub Release com notas do changelog.

## Comandos locais

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
git tag -a v0.2.0 -m "Seu React 0.2.0"
git push origin main --follow-tags
```

Testes instrumentados devem ser executados em aparelho físico antes de promover uma versão candidata para estável.

## Definição de pronto

- Código revisado e sem material bruto descartado em falhas.
- Testes relevantes aprovados.
- Android Lint sem erros.
- APK debug compilado.
- Changelog e README atualizados.
- Cenários físicos afetados registrados na release.
