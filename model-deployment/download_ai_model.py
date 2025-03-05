from transformers import AutoModelForCausalLM, AutoTokenizer

# Define the model ID
model_id = 'FYPFAST/Llama-3.2-3B-Instruct-PEP8-Vulnerability-Python'

# Download the model and tokenizer
model = AutoModelForCausalLM.from_pretrained(model_id)
tokenizer = AutoTokenizer.from_pretrained(model_id)

# Save the model and tokenizer locally
model.save_pretrained("./fine-tuned-model")
tokenizer.save_pretrained("./fine-tuned-model")
