# Arquitetura

## Camadas

- Controller: recebe requisições HTTP genéricas
- Filter / Interceptor: valida API Key
- Service: regras de negócio (consumo, resolução de serviço)
- Gateway Client: responsável pelo proxy HTTP
- Repository: acesso ao banco
