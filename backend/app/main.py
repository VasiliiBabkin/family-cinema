from fastapi import FastAPI
from .contracts import CatalogItem

app = FastAPI(title="Семейный кинотеатр", version="0.1.0")

@app.get("/health")
def health():
    return {"status": "ok", "stage": 1, "database": "not_configured"}

@app.get("/catalog", response_model=list[CatalogItem])
def catalog():
    # Этап 1: серверное хранилище и авторизация ещё не подключены.
    return []
