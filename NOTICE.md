# Aviso legal e de procedência

## O que este projeto é

OpenFoot é uma **reimplementação comportamental** de um jogo de gerenciamento de futebol.
Ele foi escrito do zero a partir de especificações que descrevem comportamento observável:
fórmulas, constantes, probabilidades e fluxo de controle.

## O que este projeto não contém

Este repositório **não contém e nunca conterá**:

- código-fonte, descompilado ou não, do Brasfoot
- o executável original ou qualquer parte dele
- os arquivos de dados do jogo original (`.ban`, `.cfg`, `.ces`, `.bcf`, `.s22`, `.sbck`)
- escudos, uniformes, troféus, sons ou qualquer outra arte do jogo original
- a base de times e jogadores do jogo original

Essas extensões estão bloqueadas no `.gitignore` e na integração contínua.

## Conteúdo importado

O programa pode ler os arquivos da **sua própria instalação** do jogo original, na **sua máquina**.
Esse conteúdo:

- nunca é enviado para lugar nenhum, porque o jogo não tem nenhuma funcionalidade de rede
- fica marcado como não redistribuível, e a aplicação recusa exportá-lo ou compartilhá-lo
- continua sendo de propriedade dos seus autores originais

## Sobre a marca

"Brasfoot" é uma marca comercial de terceiros. Este projeto não é afiliado, patrocinado nem
endossado pelos autores do Brasfoot. A marca é citada aqui apenas de forma nominativa, para
descrever compatibilidade de formato de arquivo, no sentido de "lê arquivos de dados do
Brasfoot 22-23".

Se os detentores da marca se manifestarem, o projeto está disposto a renomear. Nada na arquitetura
depende do nome.

## Licenças

| Parte | Licença |
|---|---|
| Código | GPL-3.0-or-later |
| Especificações em `spec/` | CC BY-SA 4.0 |
| Conjunto de dados aberto (repositório separado) | CC BY-SA 4.0 |

## Contato

Questões de licenciamento ou de marca podem ser abertas como issue no repositório.
