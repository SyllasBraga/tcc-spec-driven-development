# Suíte ponta a ponta do Recorte A (quickstart.md §3).
# Uso: hurl --test --variable host=http://localhost:8080 --variable run_id=$(date +%s) src/test/hurl/recorte-a.hurl

# 1. Cadastro de A -> 201, token presente, bio/image nulos
POST {{host}}/api/users
Content-Type: application/json
{"user":{"username":"jake-{{run_id}}","email":"jake.{{run_id}}@example.com","password":"senha-ficticia-a"}}
HTTP 201
[Asserts]
header "Content-Type" contains "application/json"
header "Content-Type" contains "charset=UTF-8"
header "Location" == "/api/user"
jsonpath "$.user.email" == "jake.{{run_id}}@example.com"
jsonpath "$.user.username" == "jake-{{run_id}}"
jsonpath "$.user.token" exists
jsonpath "$.user.bio" == null
jsonpath "$.user.image" == null
jsonpath "$.user.password" not exists

# Cadastro de B (alvo dos perfis)
POST {{host}}/api/users
Content-Type: application/json
{"user":{"username":"celeb-{{run_id}}","email":"celeb.{{run_id}}@example.com","password":"senha-ficticia-b"}}
HTTP 201

# 2. Cadastro repetido -> 409
POST {{host}}/api/users
Content-Type: application/json
{"user":{"username":"jake-{{run_id}}","email":"JAKE.{{run_id}}@example.com","password":"senha-ficticia-a"}}
HTTP 409
[Asserts]
header "Content-Type" contains "charset=UTF-8"
jsonpath "$.errors.email[0]" == "has already been taken"

# 3. Cadastro sem campos -> 422 com email, username e password
POST {{host}}/api/users
Content-Type: application/json
{"user":{}}
HTTP 422
[Asserts]
header "Content-Type" contains "charset=UTF-8"
jsonpath "$.errors.email[0]" == "can't be blank"
jsonpath "$.errors.username[0]" == "can't be blank"
jsonpath "$.errors.password[0]" == "can't be blank"

# 4. Login com senha errada -> 401
POST {{host}}/api/users/login
Content-Type: application/json
{"user":{"email":"jake.{{run_id}}@example.com","password":"errada"}}
HTTP 401
[Asserts]
header "Content-Type" contains "charset=UTF-8"
jsonpath "$.errors.credentials[0]" == "invalid"

# 5. Login correto -> 200, novo token
POST {{host}}/api/users/login
Content-Type: application/json
{"user":{"email":"jake.{{run_id}}@example.com","password":"senha-ficticia-a"}}
HTTP 200
[Captures]
token_a: jsonpath "$.user.token"
[Asserts]
header "Content-Type" contains "charset=UTF-8"
jsonpath "$.user.token" exists
jsonpath "$.user.username" == "jake-{{run_id}}"

# 6. Usuário atual sem cabeçalho -> 401 is missing
GET {{host}}/api/user
HTTP 401
[Asserts]
header "Content-Type" contains "charset=UTF-8"
jsonpath "$.errors.token[0]" == "is missing"

# 7. Usuário atual com token -> 200
GET {{host}}/api/user
Authorization: Token {{token_a}}
HTTP 200
[Asserts]
header "Content-Type" contains "charset=UTF-8"
jsonpath "$.user.email" == "jake.{{run_id}}@example.com"
jsonpath "$.user.username" == "jake-{{run_id}}"
jsonpath "$.user.token" exists

# 8. Atualização de bio e image -> 200, demais campos mantidos
PUT {{host}}/api/user
Authorization: Token {{token_a}}
Content-Type: application/json
{"user":{"bio":"I like to skateboard","image":"https://i.stack.imgur.com/xHWG8.jpg"}}
HTTP 200
[Asserts]
header "Content-Type" contains "charset=UTF-8"
jsonpath "$.user.bio" == "I like to skateboard"
jsonpath "$.user.image" == "https://i.stack.imgur.com/xHWG8.jpg"
jsonpath "$.user.email" == "jake.{{run_id}}@example.com"
jsonpath "$.user.username" == "jake-{{run_id}}"

# 9. Perfil de outro usuário sem token -> 200, following false
GET {{host}}/api/profiles/celeb-{{run_id}}
HTTP 200
[Asserts]
header "Content-Type" contains "charset=UTF-8"
jsonpath "$.profile.username" == "celeb-{{run_id}}"
jsonpath "$.profile.bio" == null
jsonpath "$.profile.image" == null
jsonpath "$.profile.following" == false

# 10. Seguir -> 200, following true
POST {{host}}/api/profiles/celeb-{{run_id}}/follow
Authorization: Token {{token_a}}
HTTP 200
[Asserts]
header "Content-Type" contains "charset=UTF-8"
jsonpath "$.profile.username" == "celeb-{{run_id}}"
jsonpath "$.profile.following" == true

# 11. Perfil com token de quem segue -> following true
GET {{host}}/api/profiles/celeb-{{run_id}}
Authorization: Token {{token_a}}
HTTP 200
[Asserts]
jsonpath "$.profile.following" == true

# 12. Deixar de seguir -> 200, following false
DELETE {{host}}/api/profiles/celeb-{{run_id}}/follow
Authorization: Token {{token_a}}
HTTP 200
[Asserts]
header "Content-Type" contains "charset=UTF-8"
jsonpath "$.profile.following" == false

# 13. Perfil inexistente -> 404
GET {{host}}/api/profiles/inexistente-{{run_id}}
HTTP 404
[Asserts]
header "Content-Type" contains "charset=UTF-8"
jsonpath "$.errors.profile[0]" == "not found"

# 14. Prefixo Bearer -> 401
GET {{host}}/api/user
Authorization: Bearer {{token_a}}
HTTP 401
[Asserts]
header "Content-Type" contains "charset=UTF-8"
jsonpath "$.errors.token[0]" == "is invalid"
