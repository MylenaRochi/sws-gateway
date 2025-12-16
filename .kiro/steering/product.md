# Product Overview

## SWS Gateway

API Gateway desenvolvido em Spring Boot para gerenciar e rotear requisições HTTP.

### Principais Responsabilidades

- **Autenticação**: Validação de API Key via header `x-api-key`
- **Roteamento**: Identificação do serviço destino baseado no último segmento da URL
- **Monitoramento**: Registro de consumo por usuário, serviço e mês
- **Proxy**: Redirecionamento transparente para APIs cadastradas
- **Transparência**: Retorna exatamente a resposta da API destino sem alterações

### Características Técnicas

- Suporte a payloads JSON e binários (arquivos)
- Não modifica requisições nem respostas
- Funciona como proxy transparente