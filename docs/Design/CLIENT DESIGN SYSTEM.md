# 🍔 FOOD DELIVERY – CLIENT DESIGN SYSTEM (FINAL)

## 🎯 GOAL

* Friendly
* Visual-first
* Easy to order
* Modern & premium

---

# 🎨 COLOR SYSTEM

primary = #FF6B35
primary_light = #FF8A65
primary_dark = #E65100

secondary = #00C2A8
secondary_light = #00E0C6

accent = #FFD166

background = #FAFAFA
surface = #FFFFFF

text_primary = #1C1C1C
text_secondary = #6B6B6B

border = #E0E0E0

---

# 🌈 GRADIENT SYSTEM

Primary Gradient:
#FF6B35 → #FF8A65

Secondary Gradient:
#00C2A8 → #00E0C6

RULE:

* Use for buttons & CTA only
* DO NOT use full-screen gradient
* Keep gradient subtle

---

# 🔤 TYPOGRAPHY

* Font: Poppins / Roboto
* Title: 20–24sp (bold)
* Subtitle: 16–18sp (medium)
* Body: 14–16sp

---

# 📐 LAYOUT RULE

* Padding: 16dp
* Spacing: 8–12dp
* Corner radius:

    * small = 8dp
    * medium = 12dp
    * large = 16dp

---

# 🧩 COMPONENT SYSTEM

## 1. BUTTON (PRIMARY)

* Height: 48dp
* Background: gradient_primary
* Radius: 12dp
* Text: white
* Center

---

## 2. FOOD CARD (CORE COMPONENT)

Structure:

* Image (16:9)
* Name
* Price
* Rating ⭐
* Add button

Style:

* Background: white
* Radius: 16dp
* Padding: 12–16dp
* Shadow: drawable

---

## 3. CATEGORY CHIP

* Radius: 20dp
* Default: surface
* Selected: primary

---

## 4. INPUT

* EditText
* Rounded background
* Border: #E0E0E0
* Focus: primary

---

# 📱 SCREEN STRUCTURE

## HOME

* Search bar
* Category horizontal scroll
* Food grid (2 columns)

---

## FOOD DETAIL

* Large image
* Name + price
* Description
* Add to cart button

---

## CART

* Item list
* Quantity control
* Total price
* Checkout CTA

---

## ORDER TRACKING

* Timeline
* Status
* Map (optional)

---

## PROFILE

* User info
* Order history

---

# 🎨 DRAWABLE RULE

ALL UI must use drawable:

* button_gradient.xml
* card_background.xml
* input_background.xml

---

# 🤖 AI GENERATION RULE

AI MUST:

* Use only XML View system
* Use gradient for button
* Use card-based layout
* Use consistent spacing
* Use 2-column grid

AI MUST NOT:

* Use Material components
* Hardcode colors
* Create inconsistent UI

---

# 🏗️ NAMING

component_*.xml
template_*.xml
drawable_*.xml

---

# 🚀 GOAL

* Modern UI
* Scalable
* AI-friendly
* Clean & consistent
