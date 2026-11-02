# Observações: lacunas e ambiguidades do contrato (RealWorld, recorte A)

Registro das falhas dos testes Hurl que parecem vir do contrato, e não de erro do modelo. Os testes não foram alterados.

Referências: `realworld-recorte-a.md` (contrato entregue aos agentes), `~/tcc-oracle/*.hurl` (oráculo oculto, commit `c5aecdf9`).

## 1. Campos `bio` e `image` com string vazia

- **Teste:** `auth.hurl`, linhas 84-98 e 177-184. Envia `"bio": ""` e `"image": ""` e espera `null` no PUT e no GET.
- **Contrato:** o OpenAPI marca `bio` e `image` como `["string", "null"]`, mas nada diz que string vazia deve virar `null`. A prosa mostra só valores preenchidos.
- **Evidência:** os dois grupos (SDD e ad-hoc) falharam nas 4 asserções, guardando `""`.
- **Classificação:** lacuna do contrato. Não contar como formato do modelo.

## 2. Tamanho mínimo da senha

- **Teste:** `errors_auth.hurl`, linhas 197-204. `PUT /api/user` com `"short7c"` (7 caracteres) deve retornar 422. Linha 204 falhou com 200.
- **Contrato:** nenhuma regra de tamanho de senha em lugar nenhum.
- **Classificação:** lacuna do contrato, categoria validação.

## 3. Mensagem de campo vazio

- **Contrato, prosa** (`error-handling`, linha 222): `"can't be empty"`.
- **Contrato, OpenAPI** (linha 461) e **testes**: `"can't be blank"`.
- **Classificação:** contradição interna do contrato. Quem seguir a prosa falha nos testes de campo vazio.

## 4. Status de duplicidade (409 ou 422)

- **Contrato, prosa** (`error-handling`, linha 216): "If a request fails any validations, expect a 422".
- **Contrato, OpenAPI** (linhas 281 e 443): `409` para `ConflictError`, com `"has already been taken"`.
- **Testes:** exigem 409 em `errors_auth.hurl`, linhas 62-77.
- **Classificação:** ambiguidade. Os testes seguem o OpenAPI. Se um grupo escolher 422 pela prosa, falha nesses testes.

## 5. Chave de erro do login

- **Teste:** `errors_auth.hurl`, linha 113. Espera `$.errors.credentials[0] == "invalid"`.
- **Contrato:** a chave `credentials` não aparece em nenhum lugar.
- **Evidência:** essa asserção falhou numa execução (porta 8080) e passou na seguinte (porta 8081), o que indica mudança da implementação entre execuções.
- **Classificação:** lacuna do contrato.

## Tratamento proposto

- Registrar as falhas dessas lacunas em categoria própria, separada de "formato de resposta" e "validação/erros".
- Se o contrato for corrigido para os dois grupos, a correção deve ser feita antes da primeira execução oficial e aplicada igual nos templates e nas cópias. Itens a escrever: tamanho mínimo de senha, mensagem padrão de campo vazio, status de duplicidade e chave de erro do login.
- Qualquer mudança no contrato precisa ser registrada aqui, com a data.
