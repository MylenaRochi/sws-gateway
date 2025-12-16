# Contexto do Projeto

Este projeto é um API Gateway desenvolvido em Spring Boot.

Responsabilidades principais:
- Receber requisições HTTP de clientes
- Validar API Key enviada no header `x-api-key`
- Identificar o serviço de destino com base no último segmento da URL
- Registrar consumo por usuário, serviço e mês
- Redirecionar a requisição para a API cadastrada
- Retornar exatamente a resposta recebida da API destino

O gateway não altera payloads nem respostas.
O body pode ser JSON ou binário (arquivos).
