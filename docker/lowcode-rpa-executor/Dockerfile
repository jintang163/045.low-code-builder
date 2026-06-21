FROM python:3.11-slim

WORKDIR /app

COPY lowcode-rpa-executor/requirements.txt .

RUN pip install --no-cache-dir -r requirements.txt && \
    playwright install chromium --with-deps

COPY lowcode-rpa-executor/ .

EXPOSE 8000

CMD ["python", "-m", "app.main"]
