# FoodCouriers Client - Auth + Pre-login UI/UX Demo Plan

## 1. Context from current source

### Existing visual base
- Primary brand tone is warm food-centric orange `#FF6B35` with mint secondary `#00C2A8`.
- Main app screens are mostly light, card-based, airy, and content-first.
- Existing design docs explicitly say gradients should be subtle and mainly used for CTA, not full-screen backgrounds.

### Current issues
- `activity_login.xml` and `activity_register.xml` use a full-screen gradient, which conflicts with the brighter card-based direction used in home screens.
- Auth screens use Android default icons, so the visual quality drops compared with the intended premium/friendly direction.
- There is no pre-auth storytelling layer. Users go straight into a form before understanding the app's value.
- Typography is functional but generic. It does not yet create a memorable food-brand feel.

## 2. Proposed design direction

### Working concept
**Mafuy Pantry**

This keeps the current orange-mint system but shifts the mood from "generic delivery app" to "friendly illustrated food companion".

### Visual principles
- Light background as the primary canvas, not full-screen gradient.
- Use orange as appetite/energy color and mint as freshness/trust color.
- Introduce hand-drawn food illustrations and icon stickers with rounded organic shapes.
- Use gradient only for key CTA, progress accents, and small highlight surfaces.
- Keep cards large, soft, and calm so the pre-login area connects visually to home, cart, and profile.

### Tone
- Friendly
- Modern
- Slightly playful
- Premium but not luxury

## 3. Demo flow to build first

### Screen 1: Intro / Welcome
Purpose:
Introduce the brand and emotional hook before authentication.

Layout:
- Top: soft decorative blob + illustrated courier bag / bowl / burger composition
- Middle: hero headline
- Small supporting copy about fast delivery and curated meals
- Bottom: primary CTA `Start exploring`
- Secondary text action `I already have an account`

Key message:
- Fast delivery
- Good visuals
- Easy ordering

### Screen 2: Intro / Feature highlights
Purpose:
Show 3 core promises with icon-led storytelling.

Layout:
- Horizontal pager or stacked cards
- 3 illustration chips:
  - Fresh picks
  - Track your order
  - Safe and simple checkout
- Page indicator with orange active dot and mint inactive accent
- CTA `Continue`
- Skip action in top-right

### Screen 3: Login
Purpose:
Convert after the intro flow without losing brand personality.

Layout:
- Light background with a smaller illustration header instead of full-screen gradient
- Floating auth card with clear spacing
- Email / password inputs
- Forgot password
- Primary CTA `Sign in`
- Social login placeholders styled as outlined icon buttons, but only if the feature is really planned for later
- Inline switch to register

### Screen 4: Register
Purpose:
Same system as login, but with more optimistic tone.

Layout:
- Shared header illustration family with different artwork
- Form grouped into 2 logical zones:
  - Personal info
  - Password setup
- Short trust note under CTA: terms / privacy / secure account
- Inline switch back to login

## 4. Visual system for the demo

### Color usage
- `primary / brand`: `#FF6B35`
- `primary soft`: `#FFF3ED`
- `secondary / freshness`: `#00C2A8`
- `secondary soft`: `#E9FBF7`
- `accent / appetite`: `#FFD166`
- `background`: keep near `#FAFAFA`
- `surface`: `#FFFFFF`
- `text strong`: `#1C1C1C`
- `text muted`: `#6B6B6B`

### Additional token suggestion
- `auth_hero_peach`: pale warm tint behind hero illustrations
- `auth_leaf_mint`: soft mint for supporting icon circles
- `auth_outline`: softer border than current `#E0E0E0`

These should be added as semantic colors instead of hardcoded values in layouts.

### Typography
- Keep body readable and neutral.
- Upgrade hero/title feel with a rounder, friendlier font pairing if assets allow.
- Recommended pairing for demo:
  - Heading: `Poppins SemiBold/Bold`
  - Body/UI: `Roboto Regular/Medium`

### Shape language
- Card radius: 20-24dp on auth surfaces
- Inputs: 16dp
- CTA: 16-18dp
- Illustration chips: rounded capsules / circles

### Icon/illustration style
- No emoji
- No Android built-in icons for core auth surfaces
- Use vector drawable icons with rounded stroke language
- Prefer illustrated mini-scenes:
  - ramen bowl
  - burger box
  - delivery scooter bag
  - map pin
  - receipt / order ticket

## 5. UX rules for this flow

### Pre-login
- Keep onboarding to 2 screens only. More than that is unnecessary for this product.
- Allow skip from the second screen and remembered dismissal for later opens.
- Primary CTA should always be visible in the lower safe area.

### Login/Register
- Use visible labels, not placeholder-only forms.
- Keep input height at least 56dp.
- Show clear password visibility toggle.
- Keep first error near the field, not only in a toast.
- Use one primary CTA per screen.
- Do not visually over-weigh social login if the feature is not complete.

### Motion
- Intro illustration fade/slide: 220-280ms
- Pager transition: simple slide with subtle parallax on illustration only
- CTA press: opacity/elevation change, not scale-heavy animation
- Shared auth card entrance: upward 16dp transform + fade

## 6. What should change in the current implementation

### Replace
- Full-screen auth gradient backgrounds
- Android system icons in auth form and social buttons
- Generic "Hello / Welcome Back" copy

### Keep
- Existing orange/mint base
- Light surface system
- Card-first UI language
- XML View approach

### Add
- 2 new intro layouts
- Shared auth header illustration area
- Reusable auth tokens and drawables
- Reusable icon surface background
- Pager indicator component
- Optional auth container style for login/register reuse

## 7. Proposed implementation slices

### Slice A: foundation
- Normalize auth colors to semantic tokens
- Add illustration-support drawables
- Add auth-specific text and spacing styles
- Create reusable button/icon/input surface styles

### Slice B: pre-login demo
- Build `activity_intro.xml` or `activity_onboarding.xml`
- Add 2 intro pages
- Add pager indicator and CTA footer
- Add navigation to login/register

### Slice C: auth redesign
- Rebuild login as light surface layout
- Rebuild register using the same auth shell
- Replace default icons with custom vectors
- Add proper validation/error/helper regions

### Slice D: polish
- Add motion
- Improve empty states and trust copy
- Tune spacing for smaller phones

## 8. Demo deliverables

For first demo review, I recommend showing exactly these 4 items:
- 1 annotated moodboard page: colors, illustration direction, icon direction
- 1 intro screen
- 1 feature-highlight screen
- 1 auth shell applied to both login and register

This is enough to validate the direction before expanding to forgot password or profile onboarding.

## 9. Recommendation

The safest and strongest direction is:

- Do **not** continue the full-screen orange gradient auth style.
- Move auth and pre-login into the same light premium system as home.
- Use illustration and icon quality, not heavy gradients, to create emotion.
- Keep the number of pre-login pages to 2.

## 10. If approved, next build step

I would implement in this order:
1. Add auth/prelogin tokens and drawable assets
2. Create onboarding activity with 2 pages
3. Refactor login into the new shared auth shell
4. Refactor register to reuse the same shell
5. Hook navigation flow: intro -> login/register -> main
