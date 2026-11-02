# RealWorld (Conduit): recorte A usado no experimento

Documento de referência do pesquisador. Reúne a origem, as adaptações e o conteúdo do recorte A da API Conduit usado nos dois grupos (SDD e ad-hoc).

## Origem

| Item | Valor |
|---|---|
| Repositório | https://github.com/realworld-apps/realworld |
| Commit fixado | `c5aecdf9a2e0cf68e8420a7cae55db8d06d45f87` (29/10/2026) |
| Licença | MIT (Copyright 2021 Thinkster) |
| Arquivos de origem | `specs/api/openapi.yml`; `docs/src/content/docs/specifications/backend/endpoints.md` (até "Unfollow user"); `api-response-format.md` (Users e Profile); `error-handling.md` |

O projeto não tem requisitos funcionais e não funcionais formais. A definição vem da lista de funcionalidades (`implementation-creation/features.md`), da prosa do contrato, da especificação OpenAPI e da suíte Hurl. Segundo a documentação oficial, quando a prosa e os testes divergem, valem os testes.

## Escopo do recorte A

Sete operações, das 19 da API completa (linhas 22 a 160 do `openapi.yml` original):

| Método | Rota | Autenticação |
|---|---|---|
| `POST` | `/api/users/login` | não |
| `POST` | `/api/users` | não |
| `GET` | `/api/user` | obrigatória |
| `PUT` | `/api/user` | obrigatória |
| `GET` | `/api/profiles/{username}` | opcional |
| `POST` | `/api/profiles/{username}/follow` | obrigatória |
| `DELETE` | `/api/profiles/{username}/follow` | obrigatória |

## Adaptações feitas no contrato

- A especificação OpenAPI foi reduzida aos paths do recorte e aos componentes que eles referenciam (script no apêndice).
- `info.contact` foi removido e `servers` passou de `https://api.realworld.show/api` para `http://localhost:8080/api`, para o agente não ser direcionado ao site nem à API pública de referência.
- Na prosa, o front matter foi removido, os títulos foram rebaixados e os links internos do site passaram a apontar para âncoras locais.

## Oráculo de testes (oculto aos agentes)

Arquivos Hurl do commit fixado, mantidos fora das pastas do experimento. Nenhum deles chama endpoints fora do recorte.

| Arquivo | Requisições |
|---|---|
| `specs/api/hurl/auth.hurl` | 20 |
| `specs/api/hurl/errors_auth.hurl` | 20 |
| `specs/api/hurl/profiles.hurl` | 7 |
| `specs/api/hurl/errors_profiles.hurl` | 6 |

Download (uma vez, fora das pastas do experimento):

```bash
SHA=c5aecdf9a2e0cf68e8420a7cae55db8d06d45f87
mkdir -p ~/tcc-oracle && cd ~/tcc-oracle
for f in auth errors_auth profiles errors_profiles; do
  curl -sSfL -o $f.hurl https://raw.githubusercontent.com/realworld-apps/realworld/$SHA/specs/api/hurl/$f.hurl
done
```

Execução (com banco limpo e a aplicação em `localhost:8081`):

```bash
cd ~/tcc-oracle && hurl --test --continue-on-error --jobs 1 --variable host=http://localhost:8081 --variable uid=$(date +%s) auth.hurl errors_auth.hurl profiles.hurl errors_profiles.hurl
```

## Material entregue aos agentes

O bloco abaixo é idêntico em `descricao-base.md` das pastas SDD e no prompt 1 de `prompts-adhoc.md` das pastas ad-hoc.

````markdown
# Recorte A da API Conduit (RealWorld)

## Objetivo

Implementar o backend do recorte A da API Conduit, uma plataforma de publicação de artigos no estilo do Medium. O recorte cobre:

- autenticação de usuários via JWT: cadastro e login;
- consulta e atualização do usuário autenticado (não há exclusão de usuário);
- consulta do perfil público de um usuário;
- seguir e deixar de seguir outros usuários.

Os demais recursos da API Conduit (artigos, feed, comentários, favoritos e tags) estão fora do escopo.

## Contrato da API

O contrato abaixo é obrigatório: rotas, formatos JSON, códigos de status e formato de erro. Se a descrição em prosa divergir da especificação OpenAPI, vale a especificação OpenAPI.

### Endpoints


#### Authentication Header:

You can read the authentication header from the headers of the request

`Authorization: Token jwt.token.here`

#### Authentication:

`POST /api/users/login`

Example request body:

```json
{
  "user":{
    "email": "jake@jake.jake",
    "password": "jakejake"
  }
}
```

No authentication required, returns a [User](#users-for-authentication)

Required fields: `email`, `password`

#### Registration:

`POST /api/users`

Example request body:

```json
{
  "user":{
    "username": "Jacob",
    "email": "jake@jake.jake",
    "password": "jakejake"
  }
}
```

No authentication required, returns a [User](#users-for-authentication)

Required fields: `email`, `username`, `password`

#### Get Current User

`GET /api/user`

Authentication required, returns a [User](#users-for-authentication) that's the current user

#### Update User

`PUT /api/user`

Example request body:

```json
{
  "user":{
    "email": "jake@jake.jake",
    "bio": "I like to skateboard",
    "image": "https://i.stack.imgur.com/xHWG8.jpg"
  }
}
```

Authentication required, returns the [User](#users-for-authentication)

Accepted fields: `email`, `username`, `password`, `image`, `bio`

#### Get Profile

`GET /api/profiles/:username`

Authentication optional, returns a [Profile](#profile)

#### Follow user

`POST /api/profiles/:username/follow`

Authentication required, returns a [Profile](#profile)

No additional parameters required

#### Unfollow user

`DELETE /api/profiles/:username/follow`

Authentication required, returns a [Profile](#profile)

No additional parameters required


### Formato das respostas

Make sure the right content type like `Content-Type: application/json; charset=utf-8` is correctly returned.

#### Users (for authentication)

```json
{
  "user": {
    "email": "jake@jake.jake",
    "token": "jwt.token.here",
    "username": "jake",
    "bio": null,
    "image": null
  }
}
```

#### Profile

```json
{
  "profile": {
    "username": "jake",
    "bio": "I work at statefarm",
    "image": "https://api.realworld.io/images/smiley-cyrus.jpg",
    "following": false
  }
}
```


### Erros

If a request fails any validations, expect a 422 and errors in the following format:

```json
{
  "errors":{
    "body": [
      "can't be empty"
    ]
  }
}
```

#### Other status codes:

401 for Unauthorized requests, when a request requires authentication but it isn't provided

403 for Forbidden requests, when a request may be valid but the user doesn't have permissions to perform the action

404 for Not found requests, when a resource can't be found to fulfill the request

### Especificação OpenAPI

```yaml
openapi: 3.1.0
info:
  title: RealWorld Conduit API
  description: Conduit API documentation
  license:
    name: MIT License
    url: https://opensource.org/licenses/MIT
  version: 2.0.0
tags:
- name: Profile
- name: User and Authentication
servers:
- url: http://localhost:8080/api
paths:
  /users/login:
    post:
      tags:
      - User and Authentication
      summary: Existing user login
      description: Login for existing user
      operationId: Login
      requestBody:
        $ref: '#/components/requestBodies/LoginUserRequest'
      responses:
        '200':
          $ref: '#/components/responses/UserResponse'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '422':
          $ref: '#/components/responses/GenericError'
      x-codegen-request-body-name: body
  /users:
    post:
      tags:
      - User and Authentication
      description: Register a new user
      operationId: CreateUser
      requestBody:
        $ref: '#/components/requestBodies/NewUserRequest'
      responses:
        '201':
          $ref: '#/components/responses/UserResponse'
        '409':
          $ref: '#/components/responses/ConflictError'
        '422':
          $ref: '#/components/responses/GenericError'
      x-codegen-request-body-name: body
  /user:
    get:
      tags:
      - User and Authentication
      summary: Get current user
      description: Gets the currently logged-in user
      operationId: GetCurrentUser
      responses:
        '200':
          $ref: '#/components/responses/UserResponse'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '422':
          $ref: '#/components/responses/GenericError'
      security:
      - Token: []
    put:
      tags:
      - User and Authentication
      summary: Update current user
      description: Updated user information for current user
      operationId: UpdateCurrentUser
      requestBody:
        $ref: '#/components/requestBodies/UpdateUserRequest'
      responses:
        '200':
          $ref: '#/components/responses/UserResponse'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '422':
          $ref: '#/components/responses/GenericError'
      security:
      - Token: []
      x-codegen-request-body-name: body
  /profiles/{username}:
    get:
      tags:
      - Profile
      summary: Get a profile
      description: Get a profile of a user of the system. Auth is optional
      operationId: GetProfileByUsername
      parameters:
      - name: username
        in: path
        description: Username of the profile to get
        required: true
        schema:
          type: string
      responses:
        '200':
          $ref: '#/components/responses/ProfileResponse'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '404':
          $ref: '#/components/responses/NotFound'
        '422':
          $ref: '#/components/responses/GenericError'
  /profiles/{username}/follow:
    post:
      tags:
      - Profile
      summary: Follow a user
      description: Follow a user by username
      operationId: FollowUserByUsername
      parameters:
      - name: username
        in: path
        description: Username of the profile you want to follow
        required: true
        schema:
          type: string
      responses:
        '200':
          $ref: '#/components/responses/ProfileResponse'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '404':
          $ref: '#/components/responses/NotFound'
        '422':
          $ref: '#/components/responses/GenericError'
      security:
      - Token: []
    delete:
      tags:
      - Profile
      summary: Unfollow a user
      description: Unfollow a user by username
      operationId: UnfollowUserByUsername
      parameters:
      - name: username
        in: path
        description: Username of the profile you want to unfollow
        required: true
        schema:
          type: string
      responses:
        '200':
          $ref: '#/components/responses/ProfileResponse'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '404':
          $ref: '#/components/responses/NotFound'
        '422':
          $ref: '#/components/responses/GenericError'
      security:
      - Token: []
components:
  securitySchemes:
    Token:
      type: apiKey
      description: "For accessing the protected API resources, you must have received\
        \ a a valid JWT token after registering or logging in. This JWT token must\
        \ then be used for all protected resources by passing it in via the 'Authorization'\
        \ header.\n\nA JWT token is generated by the API by either registering via\
        \ /users or logging in via /users/login.\n\nThe following format must be in\
        \ the 'Authorization' header :\n\n    Token xxxxxx.yyyyyyy.zzzzzz\n    \n"
      name: Authorization
      in: header
  requestBodies:
    LoginUserRequest:
      required: true
      description: Credentials to use
      content:
        application/json:
          schema:
            required:
            - user
            type: object
            properties:
              user:
                $ref: '#/components/schemas/LoginUser'
    NewUserRequest:
      required: true
      description: Details of the new user to register
      content:
        application/json:
          schema:
            required:
            - user
            type: object
            properties:
              user:
                $ref: '#/components/schemas/NewUser'
    UpdateUserRequest:
      required: true
      description: User details to update. At least **one** field is required.
      content:
        application/json:
          schema:
            required:
            - user
            type: object
            properties:
              user:
                $ref: '#/components/schemas/UpdateUser'
  responses:
    ConflictError:
      description: Conflict - resource already exists
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/GenericErrorModel'
          example:
            errors:
              username:
              - has already been taken
    GenericError:
      description: Unexpected error
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/GenericErrorModel'
          example:
            errors:
              title:
              - can't be blank
    NotFound:
      description: Not Found. The error key identifies the resource type (article,
        profile, comment, etc.)
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/GenericErrorModel'
          example:
            errors:
              resource:
              - not found
    ProfileResponse:
      description: Profile
      content:
        application/json:
          schema:
            required:
            - profile
            type: object
            properties:
              profile:
                $ref: '#/components/schemas/Profile'
    Unauthorized:
      description: Unauthorized
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/GenericErrorModel'
          example:
            errors:
              token:
              - is missing
    UserResponse:
      description: User
      content:
        application/json:
          schema:
            required:
            - user
            type: object
            properties:
              user:
                $ref: '#/components/schemas/User'
  schemas:
    GenericErrorModel:
      required:
      - errors
      type: object
      properties:
        errors:
          type: object
          additionalProperties:
            type: array
            items:
              type: string
    LoginUser:
      required:
      - email
      - password
      type: object
      properties:
        email:
          type: string
        password:
          type: string
          format: password
    NewUser:
      required:
      - email
      - password
      - username
      type: object
      properties:
        username:
          type: string
        email:
          type: string
        password:
          type: string
          format: password
    Profile:
      required:
      - bio
      - following
      - image
      - username
      type: object
      properties:
        username:
          type: string
        bio:
          type:
          - string
          - 'null'
        image:
          type:
          - string
          - 'null'
        following:
          type: boolean
    UpdateUser:
      type: object
      properties:
        email:
          type: string
        password:
          type: string
        username:
          type: string
        bio:
          type:
          - string
          - 'null'
        image:
          type:
          - string
          - 'null'
    User:
      required:
      - bio
      - email
      - image
      - token
      - username
      type: object
      properties:
        email:
          type: string
        token:
          type: string
        username:
          type: string
        bio:
          type:
          - string
          - 'null'
        image:
          type:
          - string
          - 'null'
```
````

## Apêndice: script de extração da especificação OpenAPI

Requer Python 3 e PyYAML. Uso: `python extrair_openapi.py openapi.yml openapi-recorte-a.yml`.

```python
"""Extrai do openapi.yml do RealWorld só os paths do recorte A e os componentes que eles referenciam."""
import sys

import yaml

PATHS_RECORTE_A = [
    "/users/login",
    "/users",
    "/user",
    "/profiles/{username}",
    "/profiles/{username}/follow",
]


def coletar_refs(no, refs):
    if isinstance(no, dict):
        for chave, valor in no.items():
            if chave == "$ref" and isinstance(valor, str):
                refs.add(valor)
            else:
                coletar_refs(valor, refs)
    elif isinstance(no, list):
        for item in no:
            coletar_refs(item, refs)


def resolver(spec, ref):
    no = spec
    for parte in ref.removeprefix("#/").split("/"):
        no = no[parte]
    return no


def main(origem, destino):
    with open(origem, encoding="utf-8") as f:
        spec = yaml.safe_load(f)

    paths = {p: spec["paths"][p] for p in PATHS_RECORTE_A}

    pendentes = set()
    coletar_refs(paths, pendentes)
    usados = set()
    while pendentes:
        ref = pendentes.pop()
        if ref in usados:
            continue
        usados.add(ref)
        coletar_refs(resolver(spec, ref), pendentes)

    componentes = {"securitySchemes": spec["components"]["securitySchemes"]}
    for ref in sorted(usados):
        _, _, tipo, nome = ref.split("/")
        componentes.setdefault(tipo, {})[nome] = spec["components"][tipo][nome]

    tags_usadas = {t for op in paths.values() for o in op.values() if isinstance(o, dict) for t in o.get("tags", [])}
    # O contato aponta para o site do RealWorld e o servidor para a API pública de referência:
    # os dois saem para o agente não consultar implementações prontas.
    info = {k: v for k, v in spec["info"].items() if k != "contact"}
    recorte = {
        "openapi": spec["openapi"],
        "info": info,
        "tags": [t for t in spec["tags"] if t["name"] in tags_usadas],
        "servers": [{"url": "http://localhost:8080/api"}],
        "paths": paths,
        "components": componentes,
    }
    with open(destino, "w", encoding="utf-8", newline="\n") as f:
        yaml.safe_dump(recorte, f, sort_keys=False, allow_unicode=True)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
```
