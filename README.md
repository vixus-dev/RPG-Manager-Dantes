# 🔥 RPG Manager: "A Decadencia"

> *"BE GONE"*  
> — Sisyphus prime

Um sistema completo de gerenciamento de sessão para RPG de mesa ambientado em um universo único com combate tático em turnos, sistema de classes, Fantasmas Nobres, mapa interativo e muito mais. Desenvolvido inteiramente em **Java + JavaFX**.

---

## 📖 Sobre o Projeto
O **RPG Manager Dante's Inferno MK2** nasceu da necessidade de gerenciar uma campanha de RPG original inspirada na Divina Comédia,ultrakill e devil may cry, com regras próprias, sistema de atributos exclusivo e um lore denso construído sessão a sessão. O software centraliza tudo que um Mestre precisa: fichas de personagem, combate tático, bestiário, loja, mapa e log de sessão tudo em tempo real.
Este é meu maior projeto pessoal até hoje, atualmente na versão **2.44.0**.

---

## ✨ Funcionalidades

### ⚔️ Sistema de Combate
```
- Gerenciamento de turnos com contador de TU (Turn Units)
- Resolução de dano com cálculo de armadura, escudo e redução
- Suporte a ações do mestre via `AcaoMestreInput`
- HUD detalhado de turno por personagem
- Log de combate em tempo real com `SessionLogger`
- Rolagem de dados integrada com `DiceRoller`
```
### 👤 Fichas de Personagem
```
- Sistema de atributos **S.P.E.C.I.A.L.I.S.T.**:
- Estatísticas derivadas: taxa crítica, dano crítico, movimento, redução de dano
- Equipamentos: arma, armadura e dois amuletos
- Inventário com itens, consumíveis e essências de inimigos
- Persistência em arquivos `.json` por personagem
```

### 🧙 Sistema de Classes
```
Cada classe possui habilidades desbloqueadas por nível e modificadores de atributo exclusivos para cada uma delas.
existem 19 variações de classes sendo que 7 delas estão implementadas
```
### 👻 Fantasmas Nobres (Sistema do Invocador)
```
O Invocador pode vincular um **Fantasma Nobre** a si mesmo.
Esse fantasma nobre é criado pelo jogador e balanceado pelo mestre, permitindo as mais variaveis habilidades customizadas  
```

### 🗺️ Mapa Tático
```
- Renderização de mapa em grid com texturas customizadas por bioma
- Posicionamento de personagens por coordenadas `(x, y)`
- Terrenos variados: lava, névoa, chão grego, neon, carvão e mais
- Suporte a objetos destrutíveis no ambiente
```

### 📚 Bestiário
```
- Banco de inimigos em `bestiario.json`
- Suporte a propriedades especiais (ex: `EXPLODIR`)
- Recompensa de XP por inimigo abatido
- Sistema de grau de dificuldade
```

### 🏪 Loja
```
- Interface de loja integrada para compra e venda de itens durante a sessão
- Suporte a moedas: Bronze, Prata e Ouro
```

### 🎲 Extras
```
- Editor de personagem em tempo real
- Teste de atributos com prompt dedicado
- Sistema de efeitos ativos (buffs/debuffs) com `EffectFactory`
- Armas únicas com mecânicas próprias
- Habilidades de boss com comportamentos especializados
- Habilidades raciais para todas as 9 raças
```
---

## 🏗️ Estrutura do Projeto

```
src/
├── br/com/dantesrpg/
│   ├── controller/          # Controllers JavaFX (UI)
│   │   ├── CombatController
│   │   ├── MapController
│   │   ├── BestiarioController
│   │   ├── LojaController
│   │   ├── SessionLogController
│   │   └── ...
│   ├── model/               # Lógica de negócio
│   │   ├── classes/         # Classes de personagem
│   │   ├── racas/           # Raças jogáveis
│   │   ├── habilidades/     # Habilidades (classe, racial, boss)
│   │   ├── armas/           # Armas únicas e de boss
│   │   ├── fantasmasnobres/ # Fantasmas Nobres do Invocador
│   │   ├── enums/           # Atributos, tipos de ação, efeito, etc.
│   │   ├── map/             # Dados de mapa e terreno
│   │   └── util/            # DiceRoller, SessionLogger, Factories
│   └── main/                # Entrypoint e CSS global
└── main/resources/
    └── data/
        ├── bestiario.json   # Banco de inimigos
        └── players/         # Fichas dos jogadores (.json)
```

---

## 🛠️ Tecnologias

- **Java 21+**
- **JavaFX** — Interface gráfica desktop
- **JSON** — Persistência de dados de personagens e bestiário
- **Maven / Gradle** — Gerenciamento de dependências

---
## ⚠️Dependencias:

O projeto utiliza assets de imagem e fontes que não estão incluídos no repositório permitindo customização de inimigos/assets. 
Certifique-se de ter os recursos na pasta correta antes de rodar.

```
> JAVA SDK 21
> JAVA FX
> JAVA Gson 2.11.0
``` 


## 📜 Personagens Ativos na Campanha
```
A campanha conta com um elenco rico de personagens jogadores, cada um com ficha própria, equipamentos e progressão, o tamanho de mesa recomendado é 4 a 6 jogadores.
devido ao alto número de aliados NPC's que podem entrar na party é necessario um cuidado especial com a escala de dificuldade escolhida.
(de 1 a 4 aliados NPC's podem ser adicionados).
```
---

## 🗒️ Histórico de Versões principais

### v2.44.0 — MK2 *(atual)*
``` 
- Adição da classe **Barbaro** com habilidades Brutais de autobuff
- Sistema de **Fantasma Nobre** de Arkos implementado com mecânicas de custo de mana, TU e cooldown
- Múltiplos refinamentos no sistema de clones
```

### v2.16.0. — MK2 *Legado*
```
- Inicio da campanha de RPG e com o teste "pratico" foi possivel ver varios pontos de refino necessarios no código
- adição do editor de personagens com grafico de "teia"
```

### v2.0.0. — MK2 *Legado*
```
- Inicio do uso de CSS e java FX para um visual melhor
- repaginada no sistema de classes para melhor divisão e manutenção de lógica
```

### v1.0.0. — MK1 *Legado*
```
- Digitalização e iniciamento do desenvolvimento da ideia base do RPG Manager
- Uso de java Swing para interface visual
```

---

## 💡 Motivação

Este projeto surgiu da vontade de criar uma experiência de RPG verdadeiramente personalizada onde as regras, o lore e a ficção fossem construídos do zero. O manager existe para que o Mestre foque no que importa: a narrativa. Toda a burocracia fica por conta do código.

---

## 📄 Licença
Projeto pessoal e de uso privado. Distribuição apenas com autorização do autor.
---

*Feito com muito energetico, sessões longas e o espírito indomavel da raça humana para arrumar bugs que surgem a cada 4 linhas ou 2 segundos de sessão* 🔥
