# 🦈 GreenShark

Um **interceptador de pacotes estilo Burp Suite para Minecraft Java Edition**, feito como mod **Fabric**. Visualize, edite e repita (replay) pacotes de rede em tempo real — tudo dentro do jogo.

Como roda **client-side**, ele enxerga os pacotes **já descriptografados** pelo próprio cliente: não precisa de proxy MITM nem sofre com a criptografia do modo online.

> ⚠️ Ferramenta educacional / de pesquisa. Use no **seu** servidor ou em testes locais. Interceptar/alterar tráfego de servidores de terceiros pode violar as regras deles e/ou dar ban.

---

## Requisitos

- **Minecraft 1.21.11** (Java Edition)
- **Java 21**
- **Fabric Loader** `>= 0.16` + **Fabric API**

## O que ele faz

- **Captura** todos os pacotes nos dois sentidos:
  - `S→C` (servidor → cliente, recebidos)
  - `C→S` (cliente → servidor, enviados)
- **Inspeciona** os campos de cada pacote por reflexão (nomes legíveis no ambiente de dev).
- **Repete (replay)** qualquer pacote com um clique.
- **Edita** os campos simples (número, texto, booleano, enum) e reenvia.
- **Intercept mode** (como no Burp): segura o pacote e você decide **Encaminhar [F]**, **Descartar [X]** ou **Editar & encaminhar**.
  - **Segurança:** `KeepAlive` e `Ping/Pong` **nunca** são segurados (senão o servidor te derruba por timeout). Ao **desligar** o intercept, tudo que estava preso é liberado automaticamente.
- **Filtro** por nome e botão para **ocultar ruído** (movimento, keep-alive, chunks…).

## Controles

| Tecla / botão | Ação |
|---|---|
| **I** | Abre/fecha a tela do interceptador (rebindável em Opções → Controles) |
| Lista à esquerda | Clique para selecionar um pacote |
| Scroll | Rola a lista |
| **Repetir (replay)** | Reenvia o pacote selecionado |
| **Editar & enviar** | Abre o editor de campos |
| **Intercept S→C / C→S** | Liga/desliga o modo "segurar pacote" |
| **F / X** | Encaminha / descarta o pacote segurado |

---

## Como rodar (modo laboratório — recomendado)

O jeito mais didático é rodar o cliente de desenvolvimento (MC 1.21.11), que usa os *mappings* legíveis (nomes reais de pacotes e campos):

```bash
./gradlew runClient
```

Isso abre um Minecraft com o mod já carregado. Entre num mundo single-player (ou conecte num servidor de teste local) e aperte **I**.

## Como gerar o `.jar` pra usar no Minecraft normal

```bash
./gradlew build
```

O mod sai em `build/libs/greenshark-0.1.0.jar`. Copie para a pasta `mods/` de uma instalação **1.21.11** com **Fabric Loader** e **Fabric API**.

> Obs.: no Minecraft de produção os nomes de classes/campos aparecem ofuscados (ex.: `class_2596`), porque o jogo roda em *mappings* intermediary. Para a experiência didática com nomes legíveis, prefira o `runClient`.

---

## Estrutura

```
com.greenshark
├── GreenSharkClient      # entrypoint: keybind, eventos, HUD
├── net/
│   ├── PacketInterceptor # handler Netty (canaliza os pacotes)
│   ├── PipelineInjector  # insere o handler no pipeline
│   ├── InterceptQueue    # fila de pacotes segurados
│   └── HeldPacket        # pacote seguro (forward/drop)
├── model/                # Direction, CapturedPacket, PacketLog
├── inspect/
│   ├── PacketInspector   # dump reflexivo dos campos
│   └── PacketEditor      # reconstrói record / muta campos
├── action/PacketActions  # replay S→C e C→S
├── gui/
│   ├── InterceptorScreen # tela principal
│   └── FieldEditScreen   # editor de campos
└── mixin/ClientConnectionAccessor  # expõe o Channel do Netty
```

## Limitações conhecidas (v0.1)

- A edição cobre campos **simples** (primitivos, `String`, `enum`). Campos complexos (listas, NBT, componentes) são mantidos como estão.
- Nomes legíveis só no ambiente de dev (`runClient`).
- Sem persistência: o histórico vive só na sessão.

## Licença

MIT.
