# 🔥 RPG Manager: "A Decadência" (Dante's Inferno Combat Manager)

Um sistema completo de gerenciamento de sessão para RPG de mesa ambientado em um universo único com combate tático em turnos, sistema de classes, Fantasmas Nobres, mapa interativo e muito mais. Desenvolvido inteiramente em **Java 21 + JavaFX 21** com persistência em **Gson**.

---

## 📖 Sobre o Projeto
O **RPG Manager Dante's Inferno MK3** nasceu da necessidade de gerenciar uma campanha de RPG original inspirada na Divina Comédia, *Ultrakill* e *Devil May Cry*, com regras próprias, sistema de atributos exclusivo e um lore denso construído sessão a sessão. O software centraliza tudo que um Mestre precisa: fichas de personagem, combate tático, bestiário, loja, mapa e log de sessão tudo em tempo real.

O projeto atual (MK3) consolidou e refatorou as engines da geração anterior, separando logicamente os subsistemas de combate, roster de jogadores e renderização de mapa tático.

---

## ✨ Funcionalidades

### ⚔️ Sistema de Combate Tático
* **Iniciativa baseada em TU (Tempo de Unidade):** Gerenciamento dinâmico de turnos onde o menor TU dita o ator da vez.
* **Pipeline de Resolução de Ação:** Processamento modular de dano (críticos, modificadores raciais e modos de ataque) integrado ao `CombatManager`.
* **Distribuição de Dano Dinâmica:** Aplicação automática contra escudos (Escudo de Sangue ➔ Escudo Normal ➔ HP) e cálculo de recuo (Knockback) integrado com o peso físico da entidade.
* **Efeitos de Combate (Buffs/Debuffs/DoTs):** Gerenciamento automático de duração e efeitos (como sangramento, stuns, venenos e marcas) via `EffectFactory`.

### 👤 Fichas de Personagem
* **Sistema de atributos S.P.E.C.I.A.L.I.S.T.:** `FORCA`, `PERCEPCAO`, `ENDURANCE`, `CARISMA`, `INTELIGENCIA`, `DESTREZA`, `SORTE`, `INSPIRACAO`, `SAGACIDADE`, `TOPOR`.
* **Estatísticas Derivadas:** Taxa crítica, dano crítico, movimento base, redução de dano de topor, redução de DoT e resistência a controle de grupo.
* **Equipamentos Completos:** Slots para armas (melee/ranged), armadura e dois amuletos que recalculam os atributos finais dinamicamente.
* **Persistência em JSON:** Fichas persistidas e editadas via arquivos JSON em tempo real.

### 🧙 Classes Jogáveis (10 Classes)
Habilidades exclusivas desbloqueadas por nível e modificadores de atributos específicos:
* Bárbaro, Campeão, Feiticeiro, Ilusionista, Invocador, Ladino, Mestre das Balas, Paladino, Pugilista e Alquimista.

### 🧬 Raças Jogáveis (10 Raças)
Mecânicas raciais exclusivas implementadas através de hooks de ciclo de vida (ex: transformações, gatilhos ao receber dano ou abater alvos):
* Anão, Anjo Caído, Elfo, Half-Angel, Half-Demon, Humano, Lobisomem, Marionette, Vampiro e Orc.

### 👻 Fantasmas Nobres (14 FNs)
Habilidades extraordinárias e transformações invocadas pelos personagens, possuindo hooks de eventos específicos (como crítico e dano causado).

### 🗺️ Mapa Tático Reativo
* Renderização em grid programático com suporte a tokens de jogadores e inimigos.
* Áreas de Efeito (AoE) dinâmicas (círculo, quadrado, cone, linha).
* Efeitos de solo integrados (lava, névoa, solo ácido, etc.) e objetos destrutíveis em campo.
* Expansões de Domínio com mecânicas de fusão e limpeza do grid.

### 🏪 Sistema de Lojas & Inventário
* Interface visual completa para compras e vendas de equipamentos e consumíveis utilizando o sistema de moedas de Bronze, Prata e Ouro.
* Catálogo dinâmico alimentado por arquivos JSON locais.

---

## 🏗️ Estrutura do Projeto

O projeto utiliza um design modular focado na separação de responsabilidades (MVC) e na injeção de serviços:

```
src/main/java/br/com/dantesrpg/
├── main/                   # Entrypoint do aplicativo (Launcher / Main)
├── model/                  # Regras de negócio e representação das entidades
│   ├── classes/            # Definição das classes jogáveis
│   ├── racas/              # Lógicas e hooks de eventos raciais
│   ├── habilidades/        # Habilidades genéricas, de classe, raça e bosses
│   ├── fantasmasnobres/    # Fantasmas Nobres
│   ├── combat/             # Subsistemas de combate (DamageCalculator, Applicator, etc.)
│   ├── map/                # Sistema de mapa e dados de terreno
│   └── util/               # Utilitários (FileLoader, factories e ImageCache)
├── controller/             # Lógica e coordenação das telas do JavaFX
│   ├── service/            # Serviços delegados (BestiarioSpawn, CatalogoItens, UI)
│   └── map/                # Renderizadores e manipuladores do mapa tático
│
src/main/resources/
├── br/com/dantesrpg/view/  # Arquivos FXML e style.css global
└── data/                   # Arquivos de persistência e catálogos (JSON)
    ├── Lojas/              # Inventários das lojas
    ├── players/            # Saves dos personagens jogadores
    ├── armas.json          # Catálogo geral de armas
    ├── armaduras.json      # Catálogo geral de armaduras
    ├── amuletos.json       # Catálogo geral de amuletos
    └── consumiveis.json    # Catálogo geral de consumíveis
```

---

## 🛠️ Execução e Compilação

O projeto está configurado para gerar um **JAR Executável Standalone** (tudo em um) utilizando o Maven, embutindo o runtime do JavaFX, a biblioteca Gson e todos os recursos de dados e imagem necessários.

### 🚀 Como Rodar o Aplicativo (Mais Fácil)
Se você já possui o arquivo `.jar` compilado (como o disponível nas Releases do GitHub):
1. Certifique-se de ter o **Java Runtime Environment (JRE) 21** ou superior instalado na sua máquina.
2. Dê um **duplo clique** no arquivo executável:
   `RPG_Manager_Decadencia_v3.0.0.jar`
3. *Alternativamente*, você pode rodar pelo terminal usando o comando:
   ```powershell
   java -jar RPG_Manager_Decadencia_v3.0.0.jar
   ```

### 📦 Como Compilar e Empacotar (Para Desenvolvedores)
Para fazer modificações no código e gerar um novo executável a partir do código fonte:
1. Abra a pasta do projeto no VS Code ou terminal.
2. Certifique-se de possuir o Maven instalado.
3. Rode o comando de compilação:
   ```powershell
   mvn clean package
   ```
4. O Maven compilará e criará o arquivo JAR executável completo na pasta `target/` com o nome `rpg-dantes-inferno-1.0-SNAPSHOT.jar` (cerca de 114 MB devido ao acoplamento das imagens e dependências).

### ⚙️ Execução em Ambiente de Desenvolvimento (Eclipse/VS Code)
Caso queira apenas rodar e testar em tempo de desenvolvimento sem gerar o pacote JAR final:
* **Via Maven:** `mvn javafx:run`
* **Via Eclipse:** O projeto mantém a compatibilidade com a configuração de classpath local e User Libraries do Eclipse.

---

## 🗒️ Histórico de Versões

### MK1 (v1.x)
* Estrutura inicial do RPG Manager desenvolvida com Java Swing.

### MK2 (v2.x)
* Migração de interface para JavaFX com estilo customizado em CSS.
* Introdução do mapa tático em grid e estruturação em JSON.
* Criação do editor de personagens visual.

### MK3 (v3.x) - *Versão Atual*
* **Refatoração Arquitetural Completa:** Desmembramento do controller principal em Serviços dedicados e Coordinators de lógica.
* **Otimização de Startup:** Divisão da inicialização em estágio leve e estágio pesado tardio com exibição imediata da janela principal sob um overlay de carregamento.
* **Consolidação de Recursos:** Migração e limpeza de caminhos estáticos da raiz para a pasta padrão `src/main/resources`.
* **Expandido de Recursos:** Inclusão de novas classes (10 totais) e raças (10 totais).

---

## 📄 Licença
Projeto pessoal e de uso privado para campanhas de RPG do autor. Distribuição e cópia não autorizadas.
