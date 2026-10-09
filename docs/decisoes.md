# Decisões técnicas

Formato: data — decisão — motivo.

## 08/10/2026 — Fase 0

1. **Estrutura em 5 módulos `calibra-*`, pacote `br.com.calibra`** — segue a seção 5 da spec. O rascunho anterior (6 módulos `dominio`, `calculo`, etc., feito antes da spec) foi apagado com o seu aval. O histórico do Git foi mantido.
2. **Versões fixadas no POM pai** (conferidas na Maven Central em 08/10/2026, só estáveis):
   - JUnit BOM 6.1.3;
   - OpenJFX 25.0.4 (última da linha 25 LTS);
   - javafx-maven-plugin 0.0.8;
   - maven-compiler-plugin 3.16.0;
   - maven-surefire-plugin 3.6.0.

   SQLite, Flyway, Jackson e PDFBox entram só nas fases em que forem usados, com nova confirmação.
3. **`-Xlint:all` no compilador** — para a regra "compila sem avisos novos" (seção 11) ser visível.
4. **`.mvn/jvm.config` com `--sun-misc-unsafe-memory-access=allow`** — o Maven 3.9.11 (biblioteca Guice interna) usa `sun.misc.Unsafe`, e o Java 25 avisa a cada build. O aviso não vem do nosso código. A opção vale só para o processo do Maven, não para o app.
5. **Rodar o app com `install` + `-pl calibra-app javafx:run`** — com `-am` o plugin do JavaFX tenta rodar no POM pai e nos outros módulos e falha por falta de `mainClass`.
6. **Testes de fumaça conferem a regra de dependência** — os módulos core, ecu, dados e relatórios falham se o JavaFX aparecer no classpath deles; o app falha se não aparecer.
7. **Golden B (seção 7.6) usa cilindrada de 2.000 cc** — a spec dá só o curso (92,8 mm), sem o diâmetro. Com 2.000 cc os valores da tabela batem (MAP 241 kPa, PR 2,91). Com 82,5 mm (1.984 cc) daria MAP 243 kPa e PR 2,94, no limite da tolerância. **Pendente:** incluir o diâmetro (≈ 82,83 mm) ou a cilindrada na spec.
8. **Dois caminhos para a vazão de ar no core (fase 1)** — padrão `Wa = P_hp / k_ar`; opcional `Wa = P_hp × AFR × BSFC / 60`. O golden A (Garrett) parte de BSFC e A/F, então testa o segundo caminho; B, C e D testam o primeiro.
9. **WiX ainda não instalado** — só é preciso na fase 2. Instalar com `winget install --id WiXToolset.WiXCLI -e` quando você autorizar.
10. **Conferência prévia dos golden** — um cálculo independente (PowerShell) com as fórmulas da seção 7.1 reproduz A, B, C e D dentro de 1%. A mesma conta está na planilha `docs/fase0/cadeia-de-calculo.xlsx`.
11. **Pasta do projeto** — a spec chama a raiz de `calibra/`. A pasta ainda se chama `projetos\injecao-app`, porque o Windows bloqueou o renome enquanto o VS Code estava com ela aberta. Renomear depois de fechar a pasta no VS Code.
