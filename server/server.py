from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from transformers import pipeline
import time

app = FastAPI()

model_name = "FYPFAST/Llama-3.2-3B-Instruct-PEP8-Vulnerability-Python"
generator = pipeline("text-generation", model=model_name)


class PromptRequest(BaseModel):
    prompt: str


@app.post("/generate")
async def generate_text(request: PromptRequest):
    if not request.prompt:
        raise HTTPException(status_code=400, detail="No prompt provided")
    print(request.prompt)
    start_time = time.time()

    output = generator([{"role": "user", "content": request.prompt}], max_new_tokens=2048, return_full_text=False)[0]

    print(output["generated_text"])
    end_time = time.time()
    response_time = end_time - start_time
    print(f"Response Time: {response_time:.2f} seconds")

    return output["generated_text"]

