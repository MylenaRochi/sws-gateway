# Requisitos Funcionais

## RF01 – Validação de API Key
- A API deve aceitar uma API Key via header `x-api-key`
- A chave deve existir, estar ativa e vinculada a um usuário

## RF02 – Identificação do Serviço
- O serviço deve ser identificado pelo último segmento da URL
- Exemplo: `/labs/api/textfy` → serviço = `textfy`

## RF03 – Encaminhamento da Requisição
- O gateway deve encaminhar:
  - Método HTTP
  - Headers
  - Query Params
  - Body (JSON ou binário)

## RF04 – Autenticação no Serviço Destino
- O tipo de autenticação deve ser definido no cadastro do serviço
- O gateway deve aplicar a autenticação automaticamente

## RF05 – Registro de Consumo
- Cada requisição válida deve gerar um consumo
- O consumo deve ser agrupado por mês e por serviço

## RF06 – Resposta Transparente
- O gateway deve retornar exatamente a resposta da API destino
